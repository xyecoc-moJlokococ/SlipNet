.PHONY: build build-full build-lite clean

GOMOBILE_TARGETS ?= android/arm,android/arm64,android/386,android/amd64

# Default: build the Lite gomobile bundle used by the VayDNS-focused APK.
build: build-lite

build-full:
	@echo "Full gomobile bundle is not built in this fork; use build-lite."

build-lite:
	gomobile bind -trimpath -target $(GOMOBILE_TARGETS) -androidapi 21 \
		-ldflags '-s -w -checklinkname=0 -extldflags -Wl,-z,max-page-size=16384,-z,common-page-size=16384' \
		-o ../app/libs/golibs-lite.aar \
		dnstt-mobile/mobile \
		vaydns-mobile/vaydns

clean:
	rm -f ../app/libs/golibs-full.aar ../app/libs/golibs-full-sources.jar
	rm -f ../app/libs/golibs-lite.aar ../app/libs/golibs-lite-sources.jar
	rm -f ../app/libs/golibs.aar ../app/libs/golibs-sources.jar
