package app.slipnet.tunnel

import android.os.ParcelFileDescriptor
import app.slipnet.util.AppLog as Log
import java.net.DatagramSocket
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Experimental Slipstream TUN path.
 *
 * This bypasses hev-socks5-tunnel and SlipstreamSocksBridge for non-SSH
 * Slipstream VPN mode. TCP flows are handled from the TUN fd and connected
 * directly to the native Slipstream local port. The old SOCKS bridge path
 * remains available as startup fallback.
 */
object SlipstreamTunBridge {
    private const val TAG = "SlipstreamTunBridge"

    @Volatile private var manager: KotlinTunnelManager? = null
    private val running = AtomicBoolean(false)
    private val lastStartMs = AtomicLong(0)
    private val lastStopMs = AtomicLong(0)

    suspend fun start(
        tunFd: ParcelFileDescriptor,
        config: KotlinTunnelConfig,
        vpnProtect: ((DatagramSocket) -> Boolean)? = null
    ): Result<Unit> {
        stop()
        val tunnelManager = KotlinTunnelManager(
            config = config,
            tunFd = tunFd,
            onSlipstreamStart = { _, _ -> SlipstreamBridge.isNativeRunning() },
            onSlipstreamStop = { },
            vpnProtect = vpnProtect
        )
        manager = tunnelManager
        lastStartMs.set(System.currentTimeMillis())
        val result = tunnelManager.start()
        if (result.isSuccess) {
            running.set(true)
            Log.i(TAG, "Started: ${dumpState("after-start")}")
        } else {
            try { tunnelManager.stop() } catch (e: Exception) { Log.w(TAG, "Cleanup after failed start: ${e.message}") }
            manager = null
            running.set(false)
            Log.e(TAG, "Start failed: ${result.exceptionOrNull()?.message}")
        }
        return result
    }

    fun stop() {
        val existing = manager
        manager = null
        running.set(false)
        if (existing != null) {
            Log.i(TAG, "Stopping: ${dumpState("before-stop")}")
            try { existing.stop() } catch (e: Exception) { Log.w(TAG, "Stop error: ${e.message}") }
        }
        lastStopMs.set(System.currentTimeMillis())
    }

    fun isRunning(): Boolean = running.get() && manager != null

    fun isClientHealthy(): Boolean =
        isRunning() && SlipstreamBridge.isNativeRunning() && SlipstreamBridge.isQuicReady()

    fun getTunnelTxBytes(): Long = manager?.getStats()?.bytesSent ?: 0L
    fun getTunnelRxBytes(): Long = manager?.getStats()?.bytesReceived ?: 0L

    fun resetTrafficStats() {
        // KotlinTunnelManager counters are per-manager; reconnect/reset recreates it.
    }

    fun dumpState(reason: String = "snapshot"): String {
        val stats = manager?.getStats()
        return "reason=$reason running=${running.get()} manager=${manager != null} " +
            "nativeRunning=${SlipstreamBridge.isNativeRunning()} quicReady=${SlipstreamBridge.isQuicReady()} " +
            "lastStartMs=${lastStartMs.get()} lastStopMs=${lastStopMs.get()} " +
            "tx=${stats?.bytesSent ?: 0} rx=${stats?.bytesReceived ?: 0} " +
            "packetsTx=${stats?.packetsSent ?: 0} packetsRx=${stats?.packetsReceived ?: 0} " +
            "activeConnections=${stats?.activeConnections ?: 0}"
    }
}
