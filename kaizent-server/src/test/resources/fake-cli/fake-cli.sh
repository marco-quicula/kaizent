#!/bin/sh
# Fake CLI tool: each command exercises one part of the container contract.
case "$1" in
  echo-stdin) cat ;;
  id) id -u ;;
  env) env | sort ;;
  ls) find "$2" | sort ;;
  cat) cat "$2" ;;
  write) printf '%s' "$3" > "$2" ;;
  link) ln -s "$2" "$3" ;;
  sleep) sleep "$2" ;;
  fail) echo "$3" >&2; exit "$2" ;;
  big) head -c "$2" /dev/zero | tr '\0' 'x' ;;
  *) echo "unknown command: $1" >&2; exit 2 ;;
esac
