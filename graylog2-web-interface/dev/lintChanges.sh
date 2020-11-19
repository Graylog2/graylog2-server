#!/usr/bin/env bash

# This script will run the lint command in the same way like the ci.
# Right now the ci is running the lint command only for changes files.
# You can run this script with `yarn lint:changes`.
# It will give you an idea of the ci result, before pushing your changes.
# You may need to set the correct permissions for this file by running `chmod +x ./dev/lintChanges.sh`

git diff --name-only --diff-filter=ACMR origin/master...|grep -E '^graylog2-web-interface/(.*).[jt]s(x)?$'|sed s,graylog2-web-interface/,,g|xargs yarn lint:path

git diff --name-only --diff-filter=ACMR origin/master...|grep -E '^graylog2-web-interface/(.*).[jt]s(x)?$'|sed s,graylog2-web-interface/,,g|xargs yarn lint:styles:path
