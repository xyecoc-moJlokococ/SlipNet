#!/system/bin/sh

# SlipNet heat diagnostics.
# Run on the phone from Termux/adb shell:
#   su
#   sh /sdcard/slipnet_hot_diag.sh

BASE="/sdcard/slipnet_hot_diag_$(date +%Y%m%d_%H%M%S)"
LIVE_STOP_FILE="$BASE/live.stop"
LIVE_PID=""
PACKAGE_NAMES="app.slipnet.lite app.slipnet"

mkdir -p "$BASE"

say() {
  echo
  echo "== $1 =="
}

pause() {
  echo
  echo "$1"
  echo "Press Enter to continue..."
  read dummy
}

find_pid() {
  for pkg in $PACKAGE_NAMES; do
    pid="$(pidof "$pkg" 2>/dev/null | awk '{print $1}')"
    if [ -n "$pid" ]; then
      echo "$pid"
      return
    fi
  done
}

run_cmd() {
  title="$1"
  shift
  echo "=== $title ==="
  echo "$ $*"
  "$@" 2>&1
  echo
}

dump_threads() {
  pid="$1"
  echo "=== THREADS pid=$pid ==="
  if [ -n "$pid" ] && [ -d "/proc/$pid/task" ]; then
    for task in /proc/"$pid"/task/*; do
      tid="$(basename "$task")"
      comm="$(cat "$task/comm" 2>/dev/null)"
      stat="$(cat "$task/stat" 2>/dev/null)"
      sched="$(cat "$task/schedstat" 2>/dev/null)"
      echo "tid=$tid comm=$comm schedstat=$sched stat=$stat"
    done
  else
    echo "SlipNet PID not found"
  fi
  echo
}

dump_thermal() {
  echo "=== THERMAL ZONES ==="
  for zone in /sys/class/thermal/thermal_zone*; do
    [ -d "$zone" ] || continue
    name="$(cat "$zone/type" 2>/dev/null)"
    temp="$(cat "$zone/temp" 2>/dev/null)"
    echo "$(basename "$zone") type=$name temp=$temp"
  done
  echo

  echo "=== CPU FREQ ==="
  for cpu in /sys/devices/system/cpu/cpu[0-9]*; do
    [ -d "$cpu" ] || continue
    cur="$(cat "$cpu/cpufreq/scaling_cur_freq" 2>/dev/null)"
    max="$(cat "$cpu/cpufreq/scaling_max_freq" 2>/dev/null)"
    gov="$(cat "$cpu/cpufreq/scaling_governor" 2>/dev/null)"
    echo "$(basename "$cpu") cur=$cur max=$max governor=$gov"
  done
  echo
}

snapshot() {
  stage="$1"
  out="$BASE/${stage}.txt"
  log="$BASE/${stage}_logcat.txt"
  pid="$(find_pid)"

  say "Saving $stage"

  {
    echo "stage=$stage"
    echo "date=$(date)"
    echo "pid=$pid"
    echo
    run_cmd "UPTIME" uptime
    run_cmd "PS SLIPNET" sh -c "ps -A -T 2>/dev/null | grep -E 'slipnet|slipstream' || true"
    if [ -n "$pid" ]; then
      run_cmd "TOP THREADS" sh -c "top -H -b -n 1 -p '$pid' 2>/dev/null || top -H -n 1 -p '$pid' 2>/dev/null || top -n 1 2>/dev/null | head -80"
      dump_threads "$pid"
    fi
    run_cmd "DUMPSYS CPUINFO" dumpsys cpuinfo
    dump_thermal
    run_cmd "SOCKSTAT" cat /proc/net/sockstat
    run_cmd "TCP FILTERED" sh -c "ss -tanpi 2>/dev/null | grep -E '10808|10809|10880|10881|:53|:853|:443|:5222|SYN-SENT|CLOSE-WAIT' || true"
    run_cmd "UDP" ss -uanpi
    run_cmd "IP ROUTE" ip route show table all
  } > "$out" 2>&1

  logcat -d -v threadtime 2>/dev/null | grep -E 'SlipstreamNative|SlipstreamBridge|SlipstreamSocksBridge|SlipNetVpnService|KotlinTunnelManager|TunnelConnection|debug:|transfer_debug|no-progress|health|ANR|FATAL EXCEPTION' > "$log" 2>&1

  echo "snapshot: $out"
  echo "logcat:   $log"
}

start_live() {
  live="$BASE/live_hot_watch.txt"
  rm -f "$LIVE_STOP_FILE"
  say "Starting live heat watch"
  echo "live: $live"

  (
    while [ ! -f "$LIVE_STOP_FILE" ]; do
      pid="$(find_pid)"
      echo
      echo "=== $(date) pid=$pid ==="
      if [ -n "$pid" ]; then
        echo "--- top threads ---"
        top -H -b -n 1 -p "$pid" 2>/dev/null | head -40
        echo "--- hottest thread comms ---"
        for task in /proc/"$pid"/task/*; do
          tid="$(basename "$task")"
          comm="$(cat "$task/comm" 2>/dev/null)"
          sched="$(cat "$task/schedstat" 2>/dev/null)"
          echo "tid=$tid comm=$comm schedstat=$sched"
        done | head -120
      else
        echo "SlipNet PID not found"
      fi
      echo "--- cpuinfo top ---"
      dumpsys cpuinfo 2>/dev/null | head -40
      echo "--- thermal ---"
      for zone in /sys/class/thermal/thermal_zone*; do
        [ -d "$zone" ] || continue
        echo "$(basename "$zone") $(cat "$zone/type" 2>/dev/null) $(cat "$zone/temp" 2>/dev/null)"
      done
      echo "--- slipstream log tail ---"
      logcat -d -v threadtime 2>/dev/null | grep -E 'SlipstreamNative|transfer_debug|debug:|no-progress' | tail -30
      sleep 2
    done
  ) > "$live" 2>&1 &

  LIVE_PID="$!"
  echo "$LIVE_PID" > "$BASE/live.pid"
}

stop_live() {
  if [ -n "$LIVE_PID" ]; then
    say "Stopping live heat watch"
    touch "$LIVE_STOP_FILE"
    sleep 3
    wait "$LIVE_PID" 2>/dev/null
    LIVE_PID=""
  fi
}

say "SlipNet heat diagnostic session"
echo "Output directory: $BASE"
echo "Important: enable SlipNet debug logging before reproducing heat."

pause "1/4: VPN is OFF or cool/idle. Press Enter for baseline."
logcat -c 2>/dev/null
snapshot "01_cool_baseline"

pause "2/4: Turn ON Slipstream with the hot settings. Press Enter as soon as it connects."
snapshot "02_connected_start"
start_live

pause "3/4: Wait until the phone is clearly hot or performance drops. Press Enter while it is still hot."
snapshot "03_hot_reproduced"
stop_live

pause "4/4: Turn VPN OFF or switch to a cool mode. Press Enter after 30-60s."
snapshot "04_after_stop_or_cooldown"

say "Done"
echo "All files are in:"
echo "$BASE"
