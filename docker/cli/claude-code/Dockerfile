# The Claude Code image for Kaizent's cli adapter (ADR-0030, spec 0004 T10). Built locally.
# NOT VALIDATED: the claude-code preset has not been validated against this image yet (the
# flags are defined after the local discovery). Kaizent publishes no images. The tool runs
# as the non-root user 10001, with a read-only root file system; only /tmp (a temporary
# file system) and /scratch are writable.
FROM node:24-slim

# Pinned: the npm "latest" version when the image was written; not validated yet.
ARG CLAUDE_CODE_VERSION=2.1.319

RUN npm install -g @anthropic-ai/claude-code@${CLAUDE_CODE_VERSION} \
 && npm cache clean --force

RUN groupadd -g 10001 kaizent \
 && useradd -u 10001 -g 10001 -m -d /home/kaizent kaizent \
 && chown -R 10001:10001 /home/kaizent

COPY kaizent-claude.sh /usr/local/bin/kaizent-claude
RUN chmod 755 /usr/local/bin/kaizent-claude

USER 10001:10001
WORKDIR /workspace
