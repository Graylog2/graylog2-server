/**
 * @fileoverview Do not allow .fa class to be used on elements
 * @author Kyle Knight
 */
"use strict";

//------------------------------------------------------------------------------
// Requirements
//------------------------------------------------------------------------------

var rule = require("../../../lib/rules/graylog-prevent-fa-classname"),

    RuleTester = require("eslint").RuleTester;


//------------------------------------------------------------------------------
// Tests
//------------------------------------------------------------------------------

var ruleTester = new RuleTester();
ruleTester.run("graylog-prevent-fa-classname", rule, {

    valid: [

        // give me some code that won't trigger a warning
    ],

    invalid: [
        {
            code: "<i className=\"fa\" />",
            errors: [{
                message: "Fill me in.",
                type: "Me too"
            }]
        }
    ]
});
