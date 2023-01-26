#!/bin/sh
# First output some messages to stdout and stderr
echo Hello World
echo This message goes to stderr >&2
echo second line
echo third line

# and now keep spinning till terminated
while true; do
    sleep 20
done
