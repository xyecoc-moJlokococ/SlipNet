#!/system/bin/sh

# SlipNet phone-side diagnostics.
# Run in Termux/adb shell on the phone:
#   su
#   sh /sdcard/slipnet_phone_diag.sh

BASE="/sdcard/slipnet_diag_$(date +%Y%m%d_%H%M%S)"
LIVE_PID=""
LIVE_STOP_FILE="$BASE/live_watch.stop"

mkdir -p "$BASE"

say() {
  echo
  echo "== $1 =="
}

run_cmd() {
  title="$1"
  shift
  echo "=== $title ==="
  echo "$ $*"
  "$@" 2>&1
  echo
}

append_file() {
  title="$1"
  path="$2"
  echo "=== $title ==="
  echo "$ cat $path"
  cat "$path" 2>&1
  echo
}

snapshot() {
  stage="$1"
  out="$BASE/${stage}_snapshot.txt"
  log="$BASE/${stage}_logcat.txt"

  say "Saving snapshot: $stage"

  {
    echo "SlipNet diagnostics stage=$stage"
    echo "saved_at=$(date)"
    echo

    run_cmd "DATE" date
    run_cmd "UPTIME" uptime
    run_cmd "IP ADDR" ip addr
    run_cmd "IP ROUTE TABLE ALL" ip route show table all
    run_cmd "IP RULE" ip rule show
    run_cmd "TCP ALL" ss -tanpi
    run_cmd "UDP ALL" ss -uanpi
    append_file "SOCKSTAT4" /proc/net/sockstat
    append_file "SOCKSTAT6" /proc/net/sockstat6
    append_file "TCP_MEM" /proc/sys/net/ipv4/tcp_mem
    append_file "TCP_RMEM" /proc/sys/net/ipv4/tcp_rmem
    append_file "TCP_WMEM" /proc/sys/net/ipv4/tcp_wmem
    run_cmd "TC QDISC" tc qdisc show
    run_cmd "DUMPSYS CONNECTIVITY" dumpsys connectivity
    run_cmd "DUMPSYS NETD" dumpsys netd
    run_cmd "DUMPSYS VPN" dumpsys vpn
  } > "$out" 2>&1

  logcat -d -v threadtime > "$BASE/${stage}_logcat_all.txt" 2>&1
  grep -E 'SlipNet|Slipstream|SlipstreamSocksBridge|SlipNetVpnService|HevSocks5Tunnel|tun2socks|VpnService|netd|ConnectivityService' "$BASE/${stage}_logcat_all.txt" > "$log" 2>&1

  echo "snapshot: $out"
  echo "logcat:   $log"
}

start_live_watch() {
  live="$BASE/live_watch.txt"
  say "Starting live watch"
  echo "live watch: $live"
  rm -f "$LIVE_STOP_FILE"

  (
    while [ ! -f "$LIVE_STOP_FILE" ]; do
      echo
      echo "=== $(date) ==="
      echo "--- tcp filtered ---"
      ss -tanpi 2>&1 | grep -E '10808|10809|10880|10881|:53|:853|:443|:5222|SYN-SENT|CLOSE-WAIT'
      echo "--- sockstat ---"
      cat /proc/net/sockstat 2>&1
      echo "--- route default ---"
      ip route 2>&1 | grep default
      sleep 2
    done
  ) > "$live" 2>&1 &

  LIVE_PID="$!"
  echo "$LIVE_PID" > "$BASE/live_watch.pid"
}

stop_live_watch() {
  if [ -n "$LIVE_PID" ]; then
    say "Stopping live watch"
    touch "$LIVE_STOP_FILE"
    sleep 3
    wait "$LIVE_PID" 2>/dev/null
    LIVE_PID=""
  fi
}

pause() {
  echo
  echo "$1"
  echo "Press Enter to continue..."
  read dummy
}

say "SlipNet diagnostic session"
echo "Output directory: $BASE"

pause "1/4: Turn VPN ON and wait until speed is healthy. Do NOT start speedtest yet."
snapshot "01_good_before_upload"

pause "2/4: Wait for the speedtest upload phase. Press Enter immediately when upload starts, before waiting for degradation."
start_live_watch
snapshot "02_during_upload_start"

pause "3/4: Wait until upload speedtest finishes and degradation appears. Press Enter while VPN is still degraded."
snapshot "03_bad_after_upload"
stop_live_watch

pause "4/4: Restart VPN now. Press Enter after download/upload speed recovers."
snapshot "04_recovered_after_vpn_restart"

say "Done"
echo "All files are in:"
echo "$BASE"
