# The Antigravity CLI image for Kaizent's cli adapter (ADR-0030, spec 0004 T10). Built locally.
# NOT VALIDATED: the antigravity preset has not been validated against this image yet. The
# version, URL and SHA-512 below come from the linux_amd64 manifest read on 2026-11-14.
# Kaizent publishes no images. Root is used only while the image is built (the RUN steps
# below); the tool runs as the non-root user 10001, with a read-only root file system; only
# /tmp (a temporary file system) and /scratch are writable.
FROM node:24-slim

ARG AGY_VERSION=1.5.9
ARG AGY_URL=https://storage.googleapis.com/antigravity-public/antigravity-cli/1.5.9-4515810895593472/linux-x64/cli_linux_x64.tar.gz
ARG AGY_SHA512=d01eb13ba7750eed18907f1ae8860864371e0ad9064415e8d033d22b4bd7b9b4e4c7f962058520b452d67db1c33163d9a889d55a7dffa159596ec6cd91a87b79

# The package is pinned by URL and checked with its SHA-512: the build fails on a mismatch.
# The official installer is not used, because it reads a mutable manifest. The package is
# deleted after it is extracted. The package holds a single executable, antigravity (found
# in the D2c discovery), which is linked as agy.
RUN apt-get update \
 && apt-get install -y --no-install-recommends ca-certificates curl \
 && rm -rf /var/lib/apt/lists/* \
 && curl -fsSL "${AGY_URL}" -o /tmp/agy.tar.gz \
 && echo "${AGY_SHA512}  /tmp/agy.tar.gz" | sha512sum -c - \
 && mkdir -p /opt/antigravity \
 && tar -xzf /tmp/agy.tar.gz -C /opt/antigravity \
 && rm -f /tmp/agy.tar.gz \
 && ln -s /opt/antigravity/antigravity /usr/local/bin/agy

RUN groupadd -g 10001 kaizent \
 && useradd -u 10001 -g 10001 -m -d /home/kaizent kaizent \
 && chown -R 10001:10001 /home/kaizent

# The settings that restrict the tool's own tools, copied into a temporary HOME per call.
# toolPermission "strict" plus deny rules for every action of the CLI (read_file, write_file,
# command, unsandboxed, read_url, execute_url, mcp, each with the target "*").
# What is and is not known about them:
#  - The actions and the "*" target come from the literal table of the permissions page.
#  - The key permissions.deny comes from the text of the headless page ("under
#    permissions.allow in ~/.gemini/antigravity-cli/settings.json"); its example JSON was not seen.
#  - The CLI ignores a rule that it does not understand, without a warning, so the rules'
#    effect is NOT proven. The preset stays "not validated" until a real call that asks to
#    read a file shows it denied.
#  - Protections that do not depend on the rules: a single call, no workspace mounted, a
#    read-only root file system, no capabilities, mode strict, and headless mode denies
#    what would need approval.
COPY agy-settings.json /etc/kaizent/agy-settings.json
COPY kaizent-agy.sh /usr/local/bin/kaizent-agy
RUN chmod 644 /etc/kaizent/agy-settings.json \
 && chmod 755 /usr/local/bin/kaizent-agy

USER 10001:10001
WORKDIR /workspace
