#!/bin/sh
# Ignores SIGTERM so tests can verify that a forced kill (SIGKILL) is required to stop it.
trap '' TERM
while true; do
    sleep 20
done
