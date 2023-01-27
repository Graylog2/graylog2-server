#!/bin/sh
# First output some messages to stdout and stderr
echo Hello World
echo This message goes to stderr >&2
echo second line
echo third line


if [ $# -eq 0 ]
  # no argument, keep spinning forever
  then
      while true; do
          sleep 20
      done
  else
    # we have an argument, use it as an exit code
    exit $1
fi

