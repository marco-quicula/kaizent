#!/bin/sh
# Runs codex with its state (sessions, logs, caches, plugins) in a temporary CODEX_HOME, so
# that nothing is left from one call to the next (ADR-0030). The credential volume, mounted
# at /home/kaizent/.codex-login, keeps only the login: auth.json is copied in before the
# call and copied back only when codex renewed it. Kaizent itself never reads it.
set -eu
umask 077

LOGIN=/home/kaizent/.codex-login
CODEX_HOME=$(mktemp -d /tmp/codex-home.XXXXXX)
export CODEX_HOME

if [ -f "$LOGIN/auth.json" ]; then
  cp "$LOGIN/auth.json" "$CODEX_HOME/auth.json"
fi

status=0
codex "$@" || status=$?

if [ -f "$CODEX_HOME/auth.json" ] && ! cmp -s "$CODEX_HOME/auth.json" "$LOGIN/auth.json"; then
  cp "$CODEX_HOME/auth.json" "$LOGIN/auth.json.new"
  mv "$LOGIN/auth.json.new" "$LOGIN/auth.json"
fi
rm -rf "$CODEX_HOME"
exit "$status"
