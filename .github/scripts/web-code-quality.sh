#!/bin/sh

PROBLEM_COUNT=`jq '. | map(.errorCount + .warningCount)|add' /tmp/report.json`
PROBLEM_COUNT_STYLELINT=`jq '. | map(.warnings | length)|add' /tmp/report-stylelint.json`
CURRENT_REF=`git rev-parse HEAD`
TIMESTAMP=`git show --format=%at $CURRENT_REF|head -1`
CRC_USAGES=`grep -lr createReactClass src|wc -l`
REFLUX_USAGES=`grep -lr Reflux src|wc -l`

JS_FILES=`find src -name \*.js -o -name \*.jsx|wc -l`
TS_FILES=`find src -name \*.ts -o -name \*.tsx|wc -l`

ENZYME_TESTS=`grep -lr wrappedEnzyme src|wc -l`
TESTING_LIBRARY_TESTS=`grep -lr wrappedTestingLibrary src|wc -l`

PAYLOAD=$(cat <<- EOF
 {
    "version": "1.1",
    "host": "developermetrics",
    "job": "fix-linter-hints",
    "short_message": "Found ${PROBLEM_COUNT} ESLint and ${PROBLEM_COUNT_STYLELINT} Stylelint problems in commit ${CURRENT_REF}",
    "_problems": ${PROBLEM_COUNT},
    "_problems_stylelint": ${PROBLEM_COUNT_STYLELINT},
    "_reflux_usages": ${REFLUX_USAGES},
    "_create_react_class_usages": ${CRC_USAGES},
    "_current_ref": "${CURRENT_REF}",
    "_js_files": ${JS_FILES},
    "_ts_files": ${TS_FILES},
    "_enzyme_tests": ${ENZYME_TESTS},
    "_testing_library_tests": ${TESTING_LIBRARY_TESTS},
    "timestamp": ${TIMESTAMP}
   }
EOF
)

echo $PAYLOAD|jq .

