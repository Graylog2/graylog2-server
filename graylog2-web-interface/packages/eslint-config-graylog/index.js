module.exports = {
  extends: [
    'eslint:recommended',
    'airbnb',
    'plugin:import/errors',
    'plugin:import/warnings',
    'plugin:import/react',
    'plugin:flowtype/recommended',
    'plugin:jest-formatting/strict',
  ],
  plugins: [
    'import',
    'react-hooks',
    'flowtype',
    'jest-formatting',
  ],
  rules: {
    'arrow-body-style': 'off',
    'import/extensions': 'off',
    'import/no-extraneous-dependencies': 'off',
    'import/no-unresolved': 'off',
    'import/order': ['error', {
      groups: ['builtin', 'external', 'internal', ['sibling', 'index'], 'parent'],
      'newlines-between': 'always',
    }],
    'sort-imports': 'off', // disabled in favor of 'import/order'
    'jsx-a11y/label-has-associated-control': ['error', { assert: 'either' }],
    'max-classes-per-file': 'off',
    'max-len': 'off',
    'new-cap': 'off',
    'no-else-return': 'warn',
    'no-nested-ternary': 'warn',
    'no-restricted-imports': ['error', {
      paths: [{
        name: 'react-bootstrap',
        message: 'Please use `components/graylog` instead.',
      }, {
        name: 'create-react-class',
        message: 'Please use an ES6 or functional component instead.',
      }],
    }],
    'no-underscore-dangle': 'off',
    'object-curly-newline': ['error', { multiline: true, consistent: true }],
    'object-shorthand': ['error', 'methods'],
    'react/forbid-prop-types': 'off',
    'react/jsx-closing-bracket-location': ['warn', 'after-props'],
    'react/jsx-first-prop-new-line': ['warn', 'never'],
    'react/jsx-indent-props': ['error', 'first'],
    'react/jsx-one-expression-per-line': 'off',
    'react/jsx-props-no-spreading': 'off',
    'react/prefer-es6-class': 'off',
    'react/prefer-stateless-function': 'warn',
    'react/static-property-placement': 'off',

    'padding-line-between-statements': [
      'error',
      {
        blankLine: 'always',
        prev: ['block', 'block-like', 'cjs-export', 'class', 'export', 'import'],
        next: '*',
      },
      {
        blankLine: 'always',
        prev: '*',
        next: ['block', 'block-like', 'expression'],
      },
      {
        blankLine: 'always',
        prev: '*',
        next: 'return',
      },
      {
        blankLine: 'any',
        prev: ['export', 'import'],
        next: ['export', 'import'],
      },
      {
        blankLine: 'any',
        prev: ['let', 'const'],
        next: ['let', 'const'],
      },
    ],

    // eslint-plugin-flowtype configs, `recommended` is too weak in a couple of places:
    'flowtype/delimiter-dangle': [1, 'always-multiline'],
    'flowtype/no-weak-types': [
      2,
      {
        any: false,
      },
    ],
    'flowtype/require-valid-file-annotation': [
      2,
      'never', {
        annotationStyle: 'line',
        strict: true,
      },
    ],
    'flowtype/semi': [2, 'always'],
  },
};
