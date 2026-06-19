package app.slipnet.tunnel

import android.net.VpnService
import app.slipnet.util.AppLog as Log
import java.util.concurrent.atomic.AtomicLong

/**
 * Bridge to the Rust slipstream client library.
 * Provides all CLI parameters for the slipstream-client.
 * TUN packet processing is done in Kotlin.
 */
@Suppress("KotlinJniMissingFunction")
object SlipstreamBridge {
    private const val TAG = "SlipstreamBridge"
    const val OWNER_VPN = "vpn"
    const val OWNER_PROBE = "probe"
    const val DEFAULT_SLIPSTREAM_PORT = 1080
    const val DEFAULT_LISTEN_HOST = "127.0.0.1"

    private var isLibraryLoaded = false
    private var currentPort = DEFAULT_SLIPSTREAM_PORT
    @Volatile
    private var currentOwner: String? = null

    // Strong reference — cleared explicitly in setVpnService(null) after stopClient().
    // WeakReference would allow GC to collect the service while native code still
    // needs it for protectSocket(), causing a crash.
    @Volatile
    private var vpnServiceRef: VpnService? = null

    // In proxy-only mode there is no VPN interface, so protect() always returns
    // false.  That is harmless — no TUN exists to create a routing loop — so we
    // skip the protect call entirely and tell the Rust client "success".
    @Volatile
    var proxyOnlyMode = false
    private val protectSuccessCount = AtomicLong(0)
    private val protectFailureCount = AtomicLong(0)
    private val lastProtectMs = AtomicLong(0)

    init {
        try {
            System.loadLibrary("slipstream")
            isLibraryLoaded = true
            Log.d(TAG, "Native library loaded successfully")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Failed to load native library", e)
            isLibraryLoaded = false
        }
    }

    fun isLoaded(): Boolean = isLibraryLoaded

    /**
     * Set the VpnService reference for socket protection.
     * Must call setVpnService(null) only AFTER stopClient() completes.
     */
    fun setVpnService(service: VpnService?) {
        vpnServiceRef = service
        Log.d(TAG, "VpnService ${if (service != null) "set" else "cleared"}")
    }

    /**
     * Called from JNI to protect a socket fd.
     */
    @JvmStatic
    fun protectSocket(fd: Int): Boolean {
        lastProtectMs.set(System.currentTimeMillis())
        if (proxyOnlyMode) {
            protectSuccessCount.incrementAndGet()
            return true
        }
        if (proxyOnlyMode) return true // No VPN interface — protection not needed
        val service = vpnServiceRef
        if (service == null) {
            protectFailureCount.incrementAndGet()
            Log.e(TAG, "Cannot protect socket: VpnService not available")
            return false
        }
        val result = service.protect(fd)
        if (result) {
            protectSuccessCount.incrementAndGet()
            Log.d(TAG, "Protected socket fd=$fd")
            return true
        }

        // Slipstream is intentionally started before Builder.establish() so QUIC
        // can come up before app traffic is routed into the TUN interface. On
        // some Android builds VpnService.protect(fd) returns false until the VPN
        // interface exists. That is still safe here because SlipNet excludes its
        // own package from the VPN with addDisallowedApplication() when the
        // interface is established.
        protectFailureCount.incrementAndGet()
        Log.w(TAG, "VpnService.protect($fd) returned false; continuing because SlipNet self-excludes from VPN")
        return true
    }

    /**
     * Start the slipstream client (DNS tunnel).
     * The client will listen on the specified host:port for SOCKS5 connections.
     *
     * @param domain The domain for DNS tunneling
     * @param resolvers List of DNS resolvers
     * @param congestionControl Congestion control algorithm: "bbr" or "dcubic"
     * @param keepAliveInterval Keep-alive interval in ms
     * @param tcpListenPort TCP port to listen on
     * @param tcpListenHost TCP host to bind to
     * @param gsoEnabled Enable Generic Segmentation Offload
     * @param debugPoll Enable debug logging for DNS polling
     * @param debugStreams Enable debug logging for streams
     * @param resolverTransport DNS resolver carrier: "udp" or "tcp"
     */
    @Synchronized
    fun startClient(
        domain: String,
        resolvers: List<ResolverConfig>,
        congestionControl: String = "bbr",
        keepAliveInterval: Int = 5000,
        tcpListenPort: Int = DEFAULT_SLIPSTREAM_PORT,
        tcpListenHost: String = DEFAULT_LISTEN_HOST,
        gsoEnabled: Boolean = false,
        debugPoll: Boolean = false,
        debugStreams: Boolean = false,
        idlePollIntervalMs: Int = 10000,
        idleTimeoutMs: Int = 120000,
        resolverTransport: String = "udp",
        owner: String = OWNER_VPN
    ): Result<Unit> {
        if (!isLibraryLoaded) {
            return Result.failure(IllegalStateException("Native library not loaded"))
        }

        if (isNativeRunning()) {
            val activeOwner = currentOwner ?: OWNER_VPN
            val canReplace = activeOwner == owner || (owner == OWNER_VPN && activeOwner == OWNER_PROBE)
            if (!canReplace) {
                val message = "Slipstream client already running for $activeOwner; refusing $owner start"
                Log.w(TAG, message)
                return Result.failure(IllegalStateException(message))
            }
            Log.w(TAG, "Slipstream client already running for $activeOwner, stopping before $owner start...")
            stopClientInternal()
        } else {
            currentOwner = null
        }

        // Preferred port leaks must be visible, but they should not strand the user.
        // Keep this short since nearby loopback-port fallback can keep startup moving.
        var actualPort = tcpListenPort
        if (!waitForPortFree(actualPort, 5000)) {
            val preferredPort = actualPort
            actualPort = findAlternativePort(preferredPort)
                ?: run {
                    val message = "Port $preferredPort still in use after native stop; alternatives exhausted (${dumpState("port-leak")})"
                    Log.e(TAG, message)
                    return Result.failure(IllegalStateException(message))
                }
            Log.w(TAG, "Port $preferredPort still in use after native stop; using alternative port $actualPort")
        }

        return try {
            val nativeResolverTransport =
                if (resolverTransport.equals("tcp", ignoreCase = true)) "tcp" else "udp"
            Log.i(
                TAG,
                "Starting slipstream client on $tcpListenHost:$actualPort, domain=$domain, resolverTransport=$nativeResolverTransport"
            )
            currentPort = actualPort

            val result = nativeStartSlipstreamClient(
                domain = domain,
                resolverHosts = resolvers.map { it.host }.toTypedArray(),
                resolverPorts = resolvers.map { it.port }.toIntArray(),
                resolverAuthoritative = resolvers.map { it.authoritative }.toBooleanArray(),
                listenPort = actualPort,
                listenHost = tcpListenHost,
                congestionControl = congestionControl,
                keepAliveInterval = keepAliveInterval,
                gsoEnabled = gsoEnabled,
                debugPoll = debugPoll,
                debugStreams = debugStreams,
                idlePollInterval = idlePollIntervalMs,
                idleTimeoutMs = idleTimeoutMs,
                resolverTransport = nativeResolverTransport
            )

            when (result) {
                0 -> {
                    currentOwner = owner
                    Log.i(TAG, "Slipstream client started successfully")
                    Result.success(Unit)
                }
                -1 -> startFailure(RuntimeException("Invalid domain"))
                -2 -> startFailure(RuntimeException("Invalid resolver configuration"))
                -10 -> {
                    val nativeError = try { nativeGetLastError()?.takeIf { it.isNotEmpty() } } catch (_: Exception) { null }
                    startFailure(RuntimeException(nativeError ?: "Failed to spawn client thread"))
                }
                -11 -> {
                    // -11 means "client died before listener ready" — could be port
                    // conflict OR another startup error (UDP bind, socket protection).
                    val nativeError = try { nativeGetLastError()?.takeIf { it.isNotEmpty() } } catch (_: Exception) { null }
                    if (nativeError != null) {
                        Log.e(TAG, "Native startup error: $nativeError")
                    }
                    // Only retry on a different port if this one is actually still held.
                    if (isPortInUse(actualPort)) {
                        val detail = nativeError ?: "native client exited before listener ready"
                        retryOnAlternatePort(
                            basePort = tcpListenPort,
                            failedPort = actualPort,
                            domain = domain,
                            resolvers = resolvers,
                            congestionControl = congestionControl,
                            keepAliveInterval = keepAliveInterval,
                            tcpListenHost = tcpListenHost,
                            gsoEnabled = gsoEnabled,
                            debugPoll = debugPoll,
                            debugStreams = debugStreams,
                            idlePollIntervalMs = idlePollIntervalMs,
                            idleTimeoutMs = idleTimeoutMs,
                            resolverTransport = nativeResolverTransport,
                            owner = owner
                        ).onFailure {
                            Log.e(TAG, "Alternative port retry failed after native startup conflict: $detail", it)
                        }
                    } else {
                        val detail = nativeError ?: "unknown startup error"
                        startFailure(RuntimeException("Failed to start client: $detail"))
                    }
                }
                else -> startFailure(RuntimeException("Failed to start client: error $result"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception starting slipstream client", e)
            if (!isNativeRunning()) currentOwner = null
            Result.failure(e)
        }
    }

    private fun startFailure(error: Throwable): Result<Unit> {
        if (!isNativeRunning()) currentOwner = null
        return Result.failure(error)
    }

    /**
     * Retry starting the native client on an alternative port after a confirmed port conflict.
     * Only called when the original port is verified to still be in use after native failure.
     */
    private fun retryOnAlternatePort(
        basePort: Int,
        failedPort: Int,
        domain: String,
        resolvers: List<ResolverConfig>,
        congestionControl: String,
        keepAliveInterval: Int,
        tcpListenHost: String,
        gsoEnabled: Boolean,
        debugPoll: Boolean,
        debugStreams: Boolean,
        idlePollIntervalMs: Int,
        idleTimeoutMs: Int,
        resolverTransport: String,
        owner: String
    ): Result<Unit> {
        for (offset in 10..50 step 10) {
            val alt = basePort + offset
            if (alt == failedPort || isPortInUse(alt)) continue

            Log.i(TAG, "Retrying on alternative port $alt")
            currentPort = alt

            val result = nativeStartSlipstreamClient(
                domain = domain,
                resolverHosts = resolvers.map { it.host }.toTypedArray(),
                resolverPorts = resolvers.map { it.port }.toIntArray(),
                resolverAuthoritative = resolvers.map { it.authoritative }.toBooleanArray(),
                listenPort = alt,
                listenHost = tcpListenHost,
                congestionControl = congestionControl,
                keepAliveInterval = keepAliveInterval,
                gsoEnabled = gsoEnabled,
                debugPoll = debugPoll,
                debugStreams = debugStreams,
                idlePollInterval = idlePollIntervalMs,
                idleTimeoutMs = idleTimeoutMs,
                resolverTransport = resolverTransport
            )

            if (result == 0) {
                currentOwner = owner
                Log.i(TAG, "Slipstream client started successfully on alternative port $alt")
                return Result.success(Unit)
            }
            Log.w(TAG, "Alternative port $alt also failed (error $result)")
        }
        return startFailure(RuntimeException("Failed to listen on port $failedPort (alternatives exhausted)"))
    }

    private fun waitForPortFree(port: Int, maxWaitMs: Int): Boolean {
        if (!isPortInUse(port)) return true

        Log.w(TAG, "Port $port in use, waiting...")
        var waited = 0
        while (waited < maxWaitMs) {
            Thread.sleep(100)
            waited += 100
            if (!isPortInUse(port)) {
                Log.i(TAG, "Port $port became free after ${waited}ms")
                return true
            }
        }
        Log.e(TAG, "Port $port still in use after ${waited}ms")
        return false
    }

    private fun findAlternativePort(basePort: Int): Int? {
        for (offset in 10..50 step 10) {
            val candidate = basePort + offset
            if (!isPortInUse(candidate)) return candidate
        }
        return null
    }

    /**
     * Stop the slipstream client.
     * Sends stop signal and waits for the port to be released.
     * Synchronized to prevent double-stop race condition (onDestroy + coroutine cleanup).
     */
    @Synchronized
    fun stopClient(owner: String? = null) {
        stopClientInternal(owner)
    }

    private fun stopClientInternal(owner: String? = null) {
        if (!isLibraryLoaded) return
        val running = isNativeRunning()
        val activeOwner = currentOwner
        if (owner != null) {
            if (activeOwner != null && activeOwner != owner) {
                Log.d(TAG, "Skip stopping Slipstream client owned by $activeOwner (requested by $owner)")
                return
            }
            if (activeOwner == null && running && owner != OWNER_VPN) {
                Log.d(TAG, "Skip stopping Slipstream client with unknown owner (requested by $owner)")
                return
            }
        }
        if (!running) {
            if (owner == null || activeOwner == null || activeOwner == owner) {
                currentOwner = null
            }
            return
        }

        val port = currentPort
        Log.i(TAG, "Stopping slipstream client on port $port")
        try {
            nativeStopSlipstreamClient()
            // Native stop waits up to 3s internally. Brief check — don't block disconnect.
            // If port is still stuck, the next startClient() has port fallback.
            if (port > 0 && isPortInUse(port)) {
                Log.w(TAG, "Port $port still in use after native stop, waiting for cleanup...")
                if (!waitForPortFree(port, 5000)) {
                    Log.e(TAG, "Port $port still in use after native stop; leak suspected (${dumpState("stop-leak")})")
                }
            }
            Log.i(TAG, "Slipstream client stopped (port $port free: ${port <= 0 || !isPortInUse(port)})")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping slipstream client", e)
        } finally {
            currentOwner = null
        }
    }

    /**
     * Check if the slipstream client is running (native flag).
     */
    fun isClientRunning(): Boolean {
        if (!isLibraryLoaded) return false
        return try {
            nativeIsClientRunning()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking native running state", e)
            false
        }
    }

    /**
     * Check if the client is running AND the port is actually listening.
     * Use this for health checks after connection is established.
     */
    fun isClientHealthy(): Boolean {
        if (!isClientRunning()) return false

        // Verify the port is actually listening
        return try {
            java.net.Socket().use { socket ->
                socket.connect(java.net.InetSocketAddress("127.0.0.1", currentPort), 1000)
                true
            }
        } catch (e: Exception) {
            Log.w(TAG, "Client reports running but port $currentPort is not listening")
            false
        }
    }

    /**
     * Get the port the slipstream client is listening on.
     */
    fun getClientPort(): Int = currentPort

    fun dumpState(reason: String = "snapshot"): String {
        val running = isClientRunning()
        val quicReady = isQuicReady()
        val port = currentPort
        val portInUse = port > 0 && isPortInUse(port)
        return "reason=$reason port=$port portInUse=$portInUse nativeRunning=$running " +
            "quicReady=$quicReady owner=${currentOwner ?: "none"} proxyOnly=$proxyOnlyMode " +
            "protectSuccess=${protectSuccessCount.get()} protectFailure=${protectFailureCount.get()} " +
            "lastProtectMs=${lastProtectMs.get()}"
    }

    /**
     * Check if a port is currently in use (bound by another socket).
     * This tries to bind a server socket to the port - if it fails, the port is in use.
     * This is more reliable than trying to connect, because a stuck/abandoned listener
     * may not be accepting connections but still has the port bound.
     */
    private fun isPortInUse(port: Int): Boolean {
        return try {
            java.net.ServerSocket().use { serverSocket ->
                serverSocket.reuseAddress = true
                serverSocket.bind(java.net.InetSocketAddress("127.0.0.1", port))
                // Successfully bound - port is free
                false
            }
        } catch (e: java.net.BindException) {
            // Port is in use
            true
        } catch (e: Exception) {
            // Other error - assume port might be in use to be safe
            Log.w(TAG, "Error checking port $port: ${e.message}")
            true
        }
    }

    // Native methods - matches slipstream-client CLI parameters
    private external fun nativeStartSlipstreamClient(
        domain: String,
        resolverHosts: Array<String>,
        resolverPorts: IntArray,
        resolverAuthoritative: BooleanArray,
        listenPort: Int,
        listenHost: String,
        congestionControl: String,
        keepAliveInterval: Int,
        gsoEnabled: Boolean,
        debugPoll: Boolean,
        debugStreams: Boolean,
        idlePollInterval: Int,
        idleTimeoutMs: Int,
        resolverTransport: String
    ): Int

    private external fun nativeStopSlipstreamClient()
    private external fun nativeIsClientRunning(): Boolean
    private external fun nativeIsQuicReady(): Boolean
    private external fun nativeGetLastError(): String?

    /**
     * Check if the native client reports it's running (alias for isClientRunning).
     */
    fun isNativeRunning(): Boolean = isClientRunning()

    /**
     * Probe/scanner mode may reuse its own Slipstream instance, but must not
     * interrupt an active VPN-owned client.
     */
    fun canStartProbeClient(): Boolean {
        if (!isLibraryLoaded) return false
        return !isNativeRunning() || currentOwner == OWNER_PROBE
    }

    /**
     * Check if the QUIC connection is established and ready for streams.
     * This is true once the QUIC handshake completes after client startup.
     * Use this to ensure the tunnel is fully operational before routing traffic.
     */
    fun isQuicReady(): Boolean {
        if (!isLibraryLoaded) return false
        return try {
            nativeIsQuicReady()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking QUIC ready state", e)
            false
        }
    }
}
