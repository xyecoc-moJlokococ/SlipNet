package vaydns

import (
	"testing"

	vclient "github.com/net2share/vaydns/client"
)

func TestResolversParseTCPEntries(t *testing.T) {
	c, err := NewClient("tcp://1.1.1.1:53,tcp://8.8.8.8", "t.example.com", testPublicKey, "127.0.0.1:7000")
	if err != nil {
		t.Fatalf("NewClient: %v", err)
	}

	resolvers, err := c.resolvers()
	if err != nil {
		t.Fatalf("resolvers: %v", err)
	}
	if len(resolvers) != 2 {
		t.Fatalf("len(resolvers) = %d, want 2", len(resolvers))
	}
	for i, resolver := range resolvers {
		if resolver.ResolverType != vclient.ResolverTypeTCP {
			t.Fatalf("resolver[%d].ResolverType = %q, want %q", i, resolver.ResolverType, vclient.ResolverTypeTCP)
		}
	}
	if resolvers[0].ResolverAddr != "1.1.1.1:53" {
		t.Fatalf("resolver[0].ResolverAddr = %q, want 1.1.1.1:53", resolvers[0].ResolverAddr)
	}
	if resolvers[1].ResolverAddr != "8.8.8.8:53" {
		t.Fatalf("resolver[1].ResolverAddr = %q, want 8.8.8.8:53", resolvers[1].ResolverAddr)
	}
}

func TestResolversParseMixedTransports(t *testing.T) {
	c, err := NewClient("tcp://1.1.1.1:53,tls://9.9.9.9,https://dns.google/dns-query,8.8.8.8", "t.example.com", testPublicKey, "127.0.0.1:7000")
	if err != nil {
		t.Fatalf("NewClient: %v", err)
	}

	resolvers, err := c.resolvers()
	if err != nil {
		t.Fatalf("resolvers: %v", err)
	}
	if len(resolvers) != 4 {
		t.Fatalf("len(resolvers) = %d, want 4", len(resolvers))
	}

	wantTypes := []vclient.ResolverType{
		vclient.ResolverTypeTCP,
		vclient.ResolverTypeDOT,
		vclient.ResolverTypeDOH,
		vclient.ResolverTypeUDP,
	}
	wantAddrs := []string{
		"1.1.1.1:53",
		"9.9.9.9:853",
		"https://dns.google/dns-query",
		"8.8.8.8:53",
	}
	for i := range resolvers {
		if resolvers[i].ResolverType != wantTypes[i] {
			t.Fatalf("resolver[%d].ResolverType = %q, want %q", i, resolvers[i].ResolverType, wantTypes[i])
		}
		if resolvers[i].ResolverAddr != wantAddrs[i] {
			t.Fatalf("resolver[%d].ResolverAddr = %q, want %q", i, resolvers[i].ResolverAddr, wantAddrs[i])
		}
	}
}

const testPublicKey = "0000000000000000000000000000000000000000000000000000000000000000"
