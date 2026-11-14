#!/bin/sh
# Runs claude with its state (sessions, settings, caches) in a temporary CLAUDE_CONFIG_DIR,
# so that nothing is left from one call to the next (ADR-0030). The long-lived token comes
# only from the CLAUDE_CODE_OAUTH_TOKEN environment variable; this script never prints or
# writes it. "--json-schema-file <path>" is turned into "--json-schema <contents>", because
# claude takes the schema itself and Kaizent hands it over as a file in /scratch.
set -eu

# mktemp -d creates the folder for the user only.
CLAUDE_CONFIG_DIR=$(mktemp -d /tmp/claude-config.XXXXXX)
export CLAUDE_CONFIG_DIR

# Rebuild the arguments: each one is moved from the front to the back of the list, and the
# schema file option is replaced on the way.
count=$#
while [ "$count" -gt 0 ]; do
  arg=$1
  shift
  count=$((count - 1))
  if [ "$arg" = "--json-schema-file" ]; then
    if [ "$count" -lt 1 ]; then
      echo "kaizent-claude: --json-schema-file needs a path" >&2
      rm -rf "$CLAUDE_CONFIG_DIR"
      exit 2
    fi
    file=$1
    shift
    count=$((count - 1))
    schema=$(cat "$file")
    set -- "$@" --json-schema "$schema"
  else
    set -- "$@" "$arg"
  fi
done

status=0
claude "$@" || status=$?

rm -rf "$CLAUDE_CONFIG_DIR"
exit "$status"
