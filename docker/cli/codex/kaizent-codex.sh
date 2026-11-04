#!/bin/sh
# Runs codex with its state (sessions, logs, caches, plugins) in a temporary CODEX_HOME, so
# that nothing is left from one call to the next (ADR-0030). The credential volume, mounted
# at /home/kaizent/.codex-login, keeps only the login: auth.json is copied in before the
# call and copied back only when codex renewed it. Kaizent itself never reads it.
set -eu

LOGIN=/home/kaizent/.codex-login
# mktemp -d creates the folder for the user only.
CODEX_HOME=$(mktemp -d /tmp/codex-home.XXXXXX)
export CODEX_HOME

# The login copies are private (umask 077 only here). Codex itself runs with the default
# umask, so that the files it writes to /scratch, such as the answer, stay readable by the
# host application, which may run as another user.
if [ -f "$LOGIN/auth.json" ]; then
  (umask 077 && cp "$LOGIN/auth.json" "$CODEX_HOME/auth.json")
fi

status=0
codex "$@" || status=$?

if [ -f "$CODEX_HOME/auth.json" ] && ! cmp -s "$CODEX_HOME/auth.json" "$LOGIN/auth.json"; then
  (umask 077 && cp "$CODEX_HOME/auth.json" "$LOGIN/auth.json.new")
  mv "$LOGIN/auth.json.new" "$LOGIN/auth.json"
fi
rm -rf "$CODEX_HOME"
exit "$status"
