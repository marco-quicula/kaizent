# The Codex CLI image for Kaizent's cli adapter (ADR-0030, spec 0004). Built locally:
#   docker compose --profile cli build codex-cli
# Kaizent publishes no images. The tool runs as the non-root user 10001, with a read-only
# root file system; only /tmp (a temporary file system) and /scratch are writable.
FROM node:24-slim

# Pinned: the version that the codex preset was validated with (cli-presets.yaml).
ARG CODEX_VERSION=0.166.0

RUN apt-get update \
 && apt-get install -y --no-install-recommends ca-certificates \
 && rm -rf /var/lib/apt/lists/* \
 && npm install -g @openai/codex@${CODEX_VERSION} \
 && npm cache clean --force

# The login folder belongs to the user, so that a new volume mounted on it takes its
# ownership. It holds only the login (auth.json); kaizent-codex keeps everything else in /tmp.
RUN groupadd -g 10001 kaizent \
 && useradd -u 10001 -g 10001 -m -d /home/kaizent kaizent \
 && mkdir -p /home/kaizent/.codex-login \
 && chown -R 10001:10001 /home/kaizent

COPY kaizent-codex.sh /usr/local/bin/kaizent-codex
RUN chmod 755 /usr/local/bin/kaizent-codex

USER 10001:10001
WORKDIR /workspace
