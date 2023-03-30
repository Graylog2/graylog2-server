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
const noUnusedVarsOptions = { argsIgnorePattern: '^_' };

module.exports = {
  parser: '@babel/eslint-parser',
  env: {
    browser: true,
    jest: true,
  },
  overrides: [
    {
      files: ['*.ts', '*.tsx'],
      parser: '@typescript-eslint/parser',
      plugins: [
        '@typescript-eslint/eslint-plugin',
        '@tanstack/query',
      ],
      rules: {
        'no-undef': 'off',
        'no-use-before-define': 'off',
        '@typescript-eslint/no-use-before-define': ['error'],
        'no-unused-vars': 'off',
        '@typescript-eslint/no-unused-vars': ['error', noUnusedVarsOptions],
        'no-redeclare': 'off',
        '@typescript-eslint/no-redeclare': ['error'],
        'no-shadow': 'off',
        '@typescript-eslint/no-shadow': ['error'],
        '@typescript-eslint/consistent-type-imports': ['error', { prefer: 'type-imports' }],
      },
    },
    {
      files: ['*.js', '*.jsx'],
    },
    {
      files: [
        '*.test.js', '*.test.jsx', '*.test.ts', '*.test.tsx',
        '*.it.js', '*.it.jsx', '*.it.ts', '*.it.tsx',
      ],
      plugins: [
        'jest',
        'testing-library',
      ],
      extends: [
        'plugin:jest/recommended',
        'plugin:testing-library/react',
        'plugin:@tanstack/eslint-plugin-query/recommended',
      ],
      rules: {
        'jest/expect-expect': ['error', { assertFunctionNames: ['expect*', '(screen.)?find(All)?By*'] }],
        'react/jsx-no-constructed-context-values': 'off',
        'testing-library/no-debugging-utils': 'warn',
        'testing-library/prefer-screen-queries': 'off',
        'testing-library/render-result-naming-convention': 'off',
      },
    },
  ],
  extends: [
    'eslint:recommended',
    'airbnb',
    'plugin:compat/recommended',
    'plugin:import/errors',
    'plugin:import/warnings',
    'plugin:import/react',
    'plugin:jest-formatting/strict',
    'plugin:graylog/recommended',
  ],
  plugins: [
    'import',
    'react-hooks',
    'jest-formatting',
    'graylog',
  ],
  rules: {
    'arrow-body-style': 'off',
    camelcase: 'off',
    'function-paren-newline': 'off',
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
    'no-unused-vars': ['error', noUnusedVarsOptions],
    'no-nested-ternary': 'warn',
    'no-restricted-imports': ['error', {
      paths: [{
        name: 'react-bootstrap',
        message: 'Please use `components/bootstrap` instead.',
      }, {
        name: 'create-react-class',
        message: 'Please use an ES6 or functional component instead.',
      }, {
        name: 'jest-each',
        message: 'Please use `it.each` instead.',
      }, {
        name: 'lodash',
        message: 'Please use `lodash/<function>` instead for reduced bundle sizes.',
      }],
    }],
    'no-underscore-dangle': 'off',
    'object-curly-newline': ['error', { multiline: true, consistent: true }],
    'object-shorthand': ['error', 'methods'],
    'react/destructuring-assignment': 'off',
    'react/forbid-prop-types': 'off',
    'react/function-component-definition': 'off',
    'react/jsx-closing-bracket-location': ['warn', 'after-props'],
    'react/jsx-curly-spacing': ['warn', { when: 'never', children: true }],
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
    'import/internal-regex': '^(actions|components|contexts|domainActions|fixtures|helpers|hooks|logic|routing|stores|util|theme|views)/',
    polyfills: [
      'fetch',
      'IntersectionObserver',
      'Promise',
    ],
  },
};
