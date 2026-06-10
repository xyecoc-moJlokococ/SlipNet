package vaydns

import (
	"context"
	"fmt"
	"log"
	"net"
	"strings"
	"sync"
	"time"

	vclient "github.com/net2share/vaydns/client"
)

const defaultUTLSDistribution = "4*random,3*Firefox_120,1*Firefox_105,3*Chrome_120,1*Chrome_102,1*iOS_14,1*iOS_13"

// VaydnsClient wraps a VayDNS tunnel client with Start/Stop lifecycle.
type VaydnsClient struct {
	dnsAddr      string
	tunnelDomain string
	publicKey    string
	listenAddr   string

	dnsttCompat     bool
	maxPayload      int
	recordType      string
	maxQnameLen     int
	rps             float64
	idleTimeout     time.Duration
	keepAlive       time.Duration
	udpTimeout      time.Duration
	maxNumLabels    int
	clientIDSize    int
	resolverMode    string
	rrSpreadCount   int
	utlsFingerprint string
	socksUser       string
	socksPass       string

	mu       sync.Mutex
	running  bool
	cancel   context.CancelFunc
	listener net.Listener
	tunnel   *vclient.Tunnel
}

// NewClient creates a new VayDNS client. Transport is auto-detected from dnsAddr:
//
//   - "https://..." -> DoH
//   - "tls://host:port" -> DoT
//   - "tcp://host:port" -> plain DNS over TCP
//   - "host:port" -> UDP
func NewClient(dnsAddr, tunnelDomain, publicKey, listenAddr string) (*VaydnsClient, error) {
	if strings.TrimSpace(dnsAddr) == "" {
		return nil, fmt.Errorf("DNS resolver is required")
	}
	if strings.TrimSpace(tunnelDomain) == "" {
		return nil, fmt.Errorf("tunnel domain is required")
	}
	if strings.TrimSpace(publicKey) == "" {
		return nil, fmt.Errorf("public key is required")
	}
	if strings.TrimSpace(listenAddr) == "" {
		return nil, fmt.Errorf("listen address is required")
	}
	return &VaydnsClient{
		dnsAddr:      dnsAddr,
		tunnelDomain: tunnelDomain,
		publicKey:    publicKey,
		listenAddr:   listenAddr,
		recordType:   "txt",
	}, nil
}

func (c *VaydnsClient) SetDnsttCompat(enabled bool) {
	c.dnsttCompat = enabled
}

func (c *VaydnsClient) SetMaxPayload(size int64) {
	if size > 0 {
		c.maxPayload = int(size)
	}
}

func (c *VaydnsClient) SetRecordType(recordType string) {
	if strings.TrimSpace(recordType) != "" {
		c.recordType = strings.ToLower(strings.TrimSpace(recordType))
	}
}

func (c *VaydnsClient) SetMaxQnameLen(length int64) {
	if length > 0 {
		c.maxQnameLen = int(length)
	}
}

func (c *VaydnsClient) SetRPS(rps float64) {
	if rps > 0 {
		c.rps = rps
	}
}

func (c *VaydnsClient) SetIdleTimeout(seconds int64) {
	if seconds > 0 {
		c.idleTimeout = time.Duration(seconds) * time.Second
	}
}

func (c *VaydnsClient) SetKeepAlive(seconds int64) {
	if seconds > 0 {
		c.keepAlive = time.Duration(seconds) * time.Second
	}
}

func (c *VaydnsClient) SetUDPTimeout(ms int64) {
	if ms > 0 {
		c.udpTimeout = time.Duration(ms) * time.Millisecond
	}
}

func (c *VaydnsClient) SetMaxNumLabels(n int64) {
	if n > 0 {
		c.maxNumLabels = int(n)
	}
}

func (c *VaydnsClient) SetClientIDSize(size int64) {
	if size > 0 {
		c.clientIDSize = int(size)
	}
}

// SetResolverMode is kept for SlipNet UI compatibility. Current VayDNS
// multi-resolver routing is health-aware round-robin.
func (c *VaydnsClient) SetResolverMode(mode string) {
	c.resolverMode = strings.ToLower(strings.TrimSpace(mode))
}

func (c *VaydnsClient) SetRRSpreadCount(n int64) {
	if n > 0 {
		c.rrSpreadCount = int(n)
	}
}

func (c *VaydnsClient) SetSocksCredentials(user, pass string) {
	c.socksUser = user
	c.socksPass = pass
}

func (c *VaydnsClient) SetUTLSFingerprint(fingerprint string) {
	c.utlsFingerprint = strings.TrimSpace(fingerprint)
}

func (c *VaydnsClient) Start() error {
	c.mu.Lock()
	defer c.mu.Unlock()
	if c.running {
		return fmt.Errorf("client is already running")
	}

	ctx, cancel := context.WithCancel(context.Background())
	c.cancel = cancel
	c.running = true

	go func() {
		err := c.run(ctx)
		if err != nil && ctx.Err() == nil {
			log.Printf("vaydns client: %v", err)
		}
		c.mu.Lock()
		c.running = false
		c.listener = nil
		c.tunnel = nil
		c.mu.Unlock()
	}()
	return nil
}

func (c *VaydnsClient) Stop() {
	c.mu.Lock()
	cancel := c.cancel
	ln := c.listener
	tunnel := c.tunnel
	c.cancel = nil
	c.listener = nil
	c.tunnel = nil
	c.running = false
	c.mu.Unlock()

	if cancel != nil {
		cancel()
	}
	if ln != nil {
		_ = ln.Close()
	}
	if tunnel != nil {
		_ = tunnel.Close()
	}
}

func (c *VaydnsClient) IsRunning() bool {
	c.mu.Lock()
	defer c.mu.Unlock()
	return c.running
}

func (c *VaydnsClient) run(ctx context.Context) error {
	resolvers, err := c.resolvers()
	if err != nil {
		return err
	}

	ts, err := vclient.NewTunnelServer(c.tunnelDomain, c.publicKey)
	if err != nil {
		return err
	}
	ts.DnsttCompat = c.dnsttCompat
	ts.ClientIDSize = c.clientIDSize
	ts.MaxQnameLen = c.maxQnameLen
	ts.MaxNumLabels = c.maxNumLabels
	ts.RPS = c.rps
	ts.RecordType = c.recordType
	if c.dnsttCompat {
		ts.RecordType = "txt"
	}

	tunnel, err := vclient.NewTunnelMulti(resolvers, ts)
	if err != nil {
		return err
	}
	tunnel.IdleTimeout = c.idleTimeout
	tunnel.KeepAlive = c.keepAlive

	if err := tunnel.InitiateResolverConnection(); err != nil {
		return err
	}
	if err := tunnel.InitiateDNSPacketConn(ts.Addr); err != nil {
		tunnel.Close()
		return err
	}
	if err := tunnel.InitiateKCPConn(c.maxPayload); err != nil {
		tunnel.Close()
		return err
	}
	if err := tunnel.InitiateNoiseChannel(); err != nil {
		tunnel.Close()
		return err
	}
	if err := tunnel.InitiateSmuxSession(); err != nil {
		tunnel.Close()
		return err
	}
	defer tunnel.Close()

	localAddr, err := net.ResolveTCPAddr("tcp", c.listenAddr)
	if err != nil {
		return err
	}
	ln, err := net.ListenTCP("tcp", localAddr)
	if err != nil {
		return err
	}
	defer ln.Close()

	c.mu.Lock()
	c.listener = ln
	c.tunnel = tunnel
	c.mu.Unlock()

	go func() {
		<-ctx.Done()
		_ = ln.Close()
		_ = tunnel.Close()
	}()

	for {
		local, err := ln.Accept()
		if err != nil {
			if ctx.Err() != nil {
				return nil
			}
			if ne, ok := err.(net.Error); ok && ne.Temporary() {
				continue
			}
			return err
		}
		go func(conn *net.TCPConn) {
			defer conn.Close()
			if err := tunnel.Handle(conn); err != nil {
				log.Printf("vaydns handle: %v", err)
			}
		}(local.(*net.TCPConn))
	}
}

func (c *VaydnsClient) resolvers() ([]vclient.Resolver, error) {
	utlsSpec := defaultUTLSDistribution
	if c.utlsFingerprint != "" {
		utlsSpec = c.utlsFingerprint
	}
	utlsID, err := vclient.SampleUTLSDistribution(utlsSpec)
	if err != nil {
		return nil, err
	}

	parts := strings.Split(c.dnsAddr, ",")
	resolvers := make([]vclient.Resolver, 0, len(parts))
	for _, part := range parts {
		addr := strings.TrimSpace(part)
		if addr == "" {
			continue
		}
		switch {
		case strings.HasPrefix(addr, "https://"):
			resolver, err := vclient.NewResolver(vclient.ResolverTypeDOH, addr)
			if err != nil {
				return nil, err
			}
			resolver.UTLSClientHelloID = utlsID
			resolvers = append(resolvers, resolver)
		case strings.HasPrefix(addr, "tls://"):
			resolver, err := vclient.NewResolver(vclient.ResolverTypeDOT, withDefaultPort(strings.TrimPrefix(addr, "tls://"), "853"))
			if err != nil {
				return nil, err
			}
			resolver.UTLSClientHelloID = utlsID
			resolvers = append(resolvers, resolver)
		case strings.HasPrefix(addr, "tcp://"):
			resolver, err := vclient.NewResolver(vclient.ResolverTypeTCP, withDefaultPort(strings.TrimPrefix(addr, "tcp://"), "53"))
			if err != nil {
				return nil, err
			}
			resolvers = append(resolvers, resolver)
		default:
			resolver, err := vclient.NewResolver(vclient.ResolverTypeUDP, withDefaultPort(addr, "53"))
			if err != nil {
				return nil, err
			}
			if c.udpTimeout > 0 {
				resolver.UDPTimeout = c.udpTimeout
			}
			resolvers = append(resolvers, resolver)
		}
	}
	if len(resolvers) == 0 {
		return nil, fmt.Errorf("at least one resolver is required")
	}
	return resolvers, nil
}

func withDefaultPort(addr, port string) string {
	addr = strings.TrimSpace(addr)
	if addr == "" {
		return addr
	}
	if _, _, err := net.SplitHostPort(addr); err == nil {
		return addr
	}
	return net.JoinHostPort(strings.Trim(addr, "[]"), port)
}
