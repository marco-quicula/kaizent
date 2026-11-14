#!/bin/sh
# Runs agy with a private, temporary HOME, so that nothing is left from one call to the next
# (ADR-0030). The settings that restrict the tool's own tools (toolPermission "strict" and
# the deny rules) are copied in from the image before the call. The API key comes only from
# the GEMINI_API_KEY environment variable; this script never prints or writes it. A Google
# account login (it uses the system keyring) is not supported in a container.
set -eu

# mktemp -d creates the folder for the user only.
HOME=$(mktemp -d /tmp/agy-home.XXXXXX)
export HOME

mkdir -p "$HOME/.gemini/antigravity-cli"
cp /etc/kaizent/agy-settings.json "$HOME/.gemini/antigravity-cli/settings.json"

status=0
agy "$@" || status=$?

rm -rf "$HOME"
exit "$status"
