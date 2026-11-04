# A fake CLI tool for the DockerCliExecutor tests (spec 0004, ADR-0030). It runs as the same
# non-root user as the real CLI images (uid and gid 10001).
FROM alpine:3.22
# Like the real CLI images, the folder that the credential volume is mounted on exists and
# belongs to the user, so that a new volume takes its ownership.
RUN addgroup -g 10001 kaizent && adduser -D -u 10001 -G kaizent kaizent \
    && mkdir /home/kaizent/.tool && chown kaizent:kaizent /home/kaizent/.tool
COPY fake-cli.sh /usr/local/bin/fake-cli
RUN chmod 755 /usr/local/bin/fake-cli
USER 10001:10001
