/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
module.exports = {
  parser: 'babel-eslint',
  env: {
    browser: true,
    jest: true,
  },
  overrides: [
    {
      files: ['*.ts', '*.tsx'],
      parser: '@typescript-eslint/parser',
      plugins: ['@typescript-eslint/eslint-plugin'],
      rules: {
        'no-undef': 'off',
        'no-use-before-define': 'off',
        '@typescript-eslint/no-use-before-define': ['error'],
        'no-unused-vars': 'off',
        '@typescript-eslint/no-unused-vars': ['error'],
        'no-redeclare': 'off',
        '@typescript-eslint/no-redeclare': ['error'],
        'no-shadow': 'off',
        '@typescript-eslint/no-shadow': ['error'],
      },
    },
    {
      files: ['*.js', '*.jsx'],
    },
  ],
  extends: [
    'eslint:recommended',
    'airbnb',
    'plugin:import/errors',
    'plugin:import/warnings',
    'plugin:import/react',
    'plugin:jest-formatting/strict',
  ],
  plugins: [
    'import',
    'react-hooks',
    'jest-formatting',
  ],
  rules: {
    'arrow-body-style': 'off',
    camelcase: 'off',
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
    'react/jsx-filename-extension': [1, { extensions: ['.jsx', '.tsx'] }],
    'react/jsx-first-prop-new-line': ['warn', 'never'],
    'react/jsx-indent-props': ['error', 'first'],
    'react/jsx-one-expression-per-line': 'off',
    'react/jsx-props-no-spreading': 'off',
    'react/prefer-es6-class': 'off',
    'react/prefer-stateless-function': 'warn',
    'react/static-property-placement': 'off',

    'react-hooks/rules-of-hooks': 'error',
    'react-hooks/exhaustive-deps': 'error',

    'padding-line-between-statements': [
      'error',
      {
        blankLine: 'any',
        prev: ['let', 'const'],
        next: ['let', 'const'],
      },
      {
        blankLine: 'any',
        prev: 'expression',
        next: 'expression',
      },
      {
        blankLine: 'any',
        prev: 'export',
        next: 'export',
      },
      {
        blankLine: 'always',
        prev: ['block', 'multiline-block-like', 'cjs-export', 'class', 'multiline-expression'],
        next: '*',
      },
      {
        blankLine: 'always',
        prev: '*',
        next: ['block', 'multiline-block-like', 'class', 'multiline-expression', 'return'],
      },
    ],
  },
  settings: {
    'import/resolver': {
      webpack: {
        config: './webpack.config.js',
      },
    },
  },
};
