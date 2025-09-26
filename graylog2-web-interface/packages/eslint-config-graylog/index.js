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
import js from '@eslint/js';
import globals from 'globals';
import tseslint from 'typescript-eslint';
import react from 'eslint-plugin-react';
import jest from 'eslint-plugin-jest';
import testingLibrary from 'eslint-plugin-testing-library';
import importPlugin from 'eslint-plugin-import';
import reactHooks from 'eslint-plugin-react-hooks';
import jsxA11y from 'eslint-plugin-jsx-a11y';
import graylog from 'eslint-plugin-graylog';
import reactQuery from '@tanstack/eslint-plugin-query';
import compat from 'eslint-plugin-compat';

const ignorePattern = '^(_|ignored)';
const noUnusedVarsOptions = {
  argsIgnorePattern: ignorePattern,
  caughtErrorsIgnorePattern: ignorePattern,
  varsIgnorePattern: ignorePattern,
};

export default [
  {
    files: ['**/*.{js,mjs,cjs,ts,mts,cts,jsx,tsx}'],
    plugins: { js },
    ...js.configs.recommended,
    languageOptions: { globals: globals.browser },
  },
  tseslint.configs.recommended,
  jest.configs['flat/recommended'],
  testingLibrary.configs['flat/react'],
  importPlugin.flatConfigs.recommended,
  reactHooks.configs['recommended-latest'],
  jsxA11y.flatConfigs.recommended,
  reactQuery.configs['flat/recommended'],
  compat.configs['flat/recommended'],
  graylog.configs.recommended,
  {
    linterOptions: {
      reportUnusedDisableDirectives: true,
    },
  },
  {
    files: ['**/*.{js,mjs,cjs,jsx,mjsx,ts,tsx,mtsx}'],
    ...react.configs.flat.recommended,
    settings: {
      react: {
        version: 'detect',
      },
    },
    languageOptions: {
      globals: {
        ...globals.serviceworker,
        ...globals.browser,
      },
    },
  },
  {
    settings: {
      'import/resolver': {
        webpack: {
          config: './webpack.config.js',
        },
      },
      'import/internal-regex':
        '^(actions|components|contexts|domainActions|fixtures|helpers|hooks|logic|routing|stores|util|theme|views)/',
      polyfills: ['fetch', 'IntersectionObserver', 'Promise'],
      'testing-library/utils-module': 'wrappedTestingLibrary',
    },
  },
  // Enabling previously enabled rules which are not part of the recommended configs anymore
  {
    rules: {
      'default-param-last': 'warn',
      'dot-notation': 'error',
      'func-names': 'error',
      'global-require': 'error',
      'guard-for-in': 'error',
      'no-param-reassign': ['error', { props: true }],
      'no-plusplus': 'error',
      'no-promise-executor-return': 'error',
      'no-restricted-globals': 'error',
      'no-restricted-syntax': 'error',
      'no-script-url': 'error',
      'no-template-curly-in-string': 'error',
      'no-throw-literal': 'error',
      'no-use-before-define': 'error',
      'no-alert': 'warn',
      'no-console': 'warn',
      'class-methods-use-this': [
        'warn',
        { exceptMethods: ['componentDidMount', 'componentDidUpdate', 'componentWillUnmount'] },
      ],
      'no-await-in-loop': 'error',
      'no-loop-func': 'error',

      'import/prefer-default-export': 'error',
      'jsx-a11y/control-has-associated-label': 'error',
      'react/no-array-index-key': 'error',
      'react/no-danger': 'error',
      'react/no-unstable-nested-components': 'error',
      'react/no-unused-class-component-methods': 'error',
      'react/no-unused-prop-types': 'error',
      'react/sort-comp': 'error',
      'react/state-in-constructor': 'error',
    },
  },
  // Disabling typescript rules which are now recommended
  {
    rules: {
      '@typescript-eslint/no-explicit-any': 'off',
      '@typescript-eslint/no-empty-object-type': 'off',
      '@typescript-eslint/ban-ts-comment': 'off',
    },
  },
  {
    rules: {
      'arrow-body-style': ['error', 'as-needed'],
      camelcase: 'off',
      'import/no-named-as-default': 'off',
      'import/extensions': 'off',
      'import/no-extraneous-dependencies': 'off',
      'import/no-unresolved': 'off',
      'import/order': [
        'error',
        {
          groups: ['builtin', 'external', 'internal', ['sibling', 'index'], 'parent'],
          pathGroups: [
            {
              pattern: '@graylog/*-api',
              group: 'external',
              position: 'after',
            },
          ],
          'newlines-between': 'always',
          pathGroupsExcludedImportTypes: ['builtin'],
        },
      ],
      'sort-imports': 'off', // disabled in favor of 'import/order'
      'jsx-a11y/label-has-associated-control': ['error', { assert: 'either' }],
      'max-classes-per-file': 'off',
      'max-len': 'off',
      'new-cap': 'off',
      'no-else-return': 'warn',
      'no-nested-ternary': 'warn',
      'no-restricted-imports': [
        'error',
        {
          paths: [
            {
              name: 'react-bootstrap',
              message: 'Please use `components/bootstrap` instead.',
            },
            {
              name: 'create-react-class',
              message: 'Please use an ES6 or functional component instead.',
            },
            {
              name: 'jest-each',
              message: 'Please use `it.each` instead.',
            },
            {
              name: 'lodash',
              message: 'Please use `lodash/<function>` instead for reduced bundle sizes.',
            },
            {
              name: 'lodash/get',
              message: 'Please use optional chaining (`foo?.bar?.baz`) instead.',
            },
            {
              name: 'lodash/defaultTo',
              message: 'Please use nullish coalescing (`foo ?? 42`) instead.',
            },
            {
              name: 'lodash/max',
              message: 'Please use `Math.max` instead.',
            },
          ],
        },
      ],
      'no-underscore-dangle': 'off',
      'object-shorthand': ['error', 'methods'],
      'react/display-name': 'off',
      'react/destructuring-assignment': 'off',
      'react/forbid-prop-types': 'off',
      'react/function-component-definition': 'off',
      'react/jsx-filename-extension': [1, { extensions: ['.jsx', '.tsx'] }],
      'react/jsx-no-useless-fragment': ['error', { allowExpressions: true }],
      'react/jsx-props-no-spreading': 'off',
      'react/prefer-es6-class': 'off',
      'react/prefer-stateless-function': 'warn',
      'react/prop-types': ['off'],
      'react/static-property-placement': 'off',
      'react/require-default-props': ['warn', { functions: 'defaultArguments' }],

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
          prev: ['block', 'cjs-export', 'class'],
          next: '*',
        },
        {
          blankLine: 'always',
          prev: '*',
          next: ['block', 'class', 'return'],
        },
      ],
    },
  },
  {
    files: ['**/*.{ts,tsx}'],
    rules: {
      '@typescript-eslint/no-unused-vars': ['error', noUnusedVarsOptions],
      'no-use-before-define': 'off',
      '@typescript-eslint/no-use-before-define': 'error',
      'no-redeclare': 'off',
      '@typescript-eslint/no-redeclare': ['error'],
      'no-shadow': 'off',
      '@typescript-eslint/no-shadow': ['error'],
      '@typescript-eslint/consistent-type-imports': ['error', { prefer: 'type-imports' }],
    },
  },
  {
    files: ['**/*.test.{js,jsx,ts,tsx}'],
    rules: {
      'jest/expect-expect': [
        'error',
        { assertFunctionNames: ['expect*', '(screen.)?find(All)?By*', 'selectEvent.assertOptionExists*'] },
      ],
      'react/jsx-no-constructed-context-values': 'off',
      'testing-library/await-async-events': 'off',
      'testing-library/no-debugging-utils': 'warn',
      'testing-library/prefer-screen-queries': 'off',
      'testing-library/render-result-naming-convention': 'off',
    },
  },
];
