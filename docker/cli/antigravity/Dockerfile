# The Antigravity CLI image for Kaizent's cli adapter (ADR-0030, spec 0004 T10). Built locally.
# NOT VALIDATED: the antigravity preset has not been validated against this image yet, and
# the install step is not written yet (it comes after the D2b discovery). Kaizent publishes
# no images. Root is used only while the image is built (the RUN steps below); the tool
# runs as the non-root user 10001, with a read-only root file system; only /tmp (a
# temporary file system) and /scratch are writable.
FROM node:24-slim

# curl and ca-certificates are prerequisites of the official installer (it needs curl or wget).
RUN apt-get update \
 && apt-get install -y --no-install-recommends ca-certificates curl \
 && rm -rf /var/lib/apt/lists/*

RUN groupadd -g 10001 kaizent \
 && useradd -u 10001 -g 10001 -m -d /home/kaizent kaizent \
 && chown -R 10001:10001 /home/kaizent

USER 10001:10001
WORKDIR /workspace
