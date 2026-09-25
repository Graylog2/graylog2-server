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

// jsdom (used by the jest preset) does not expose structuredClone; ESLint 9 needs it.
const { serialize, deserialize } = require('v8');
if (typeof global.structuredClone === 'undefined') {
  global.structuredClone = (val) => deserialize(serialize(val));
}

const { RuleTester } = require('eslint');
const rule = require('../../lib/rules/no-literal-product-name');

const ruleTester = new RuleTester({
  languageOptions: {
    ecmaVersion: 2021,
    parserOptions: {
      ecmaFeatures: { jsx: true },
    },
  },
});

ruleTester.run('no-literal-product-name', rule, {
  valid: [
    { code: 'const x = "hello world"' },
    { code: 'const x = "graylog"' },
    { code: 'const x = "GrayLog"' },
    { code: "import x from 'graylog-plugin'" },
    { code: 'const x = require("graylog-plugin")' },
    { code: 'const el = <ProductName />' },
    { code: 'const el = <span>{productName} rocks</span>' },
    { code: 'const msg = `${productName} is ready`' },
  ],
  invalid: [
    {
      code: 'const x = "Welcome to Graylog"',
      errors: [{ messageId: 'noLiteralProductName' }],
    },
    {
      code: 'const x = "Graylog Data Node"',
      errors: [{ messageId: 'noLiteralProductName' }],
    },
    {
      code: 'const el = <span>Login to Graylog</span>',
      errors: [{ messageId: 'noLiteralProductName' }],
    },
    {
      code: 'const el = <Button label="Graylog Settings" />',
      errors: [{ messageId: 'noLiteralProductName' }],
    },
    {
      code: 'const msg = `Run Graylog now`',
      errors: [{ messageId: 'noLiteralProductName' }],
    },
    {
      code: 'const msg = `${prefix} Graylog ${suffix}`',
      errors: [{ messageId: 'noLiteralProductName' }],
    },
  ],
});
