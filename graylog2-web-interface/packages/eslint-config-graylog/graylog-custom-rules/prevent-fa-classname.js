var rule = require("../../../lib/rules/my-rule");
var RuleTester = require("eslint").RuleTester;

RuleTester.setDefaultConfig({
  parserOptions: {
    ecmaVersion: 6,
    ecmaFeatures: {
      jsx: true,
    },
  }
});

const ERROR_MSG_NO_FA = 'Use the `<Icon />` component instead of <i className="fa ..."/>';

const ruleTester = new RuleTester();

ruleTester.run("prevent-fa-classname", rule, {
    valid: [
      {
          code: '<i></i>',
      }
    ],
    invalid: [
        {
            code: '<i className="fa"></i>',
            errors: [{
                message: ERROR_MSG_NO_FA,
                type: 'JSXOpeningElement'
            }]
        }
    ]
});
