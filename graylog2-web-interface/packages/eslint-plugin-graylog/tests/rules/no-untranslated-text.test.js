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
const rule = require('../../lib/rules/no-untranslated-text');

const ruleTester = new RuleTester({
  languageOptions: {
    ecmaVersion: 2021,
    parserOptions: {
      ecmaFeatures: { jsx: true },
    },
  },
});

ruleTester.run('no-untranslated-text', rule, {
  valid: [
    { code: "const el = <span>{t('common:loading')}</span>" },
    { code: "const el = <Input title={t('form.title')} placeholder={t('form.placeholder')} />" },
    { code: 'const el = <span>{count} / {total}</span>' },
    { code: 'const el = <span> - 42 % </span>' },
    { code: 'const el = <Icon name="delete" type="regular" />' },
    { code: 'const el = <div id="main-content" className="pull-right" />' },
    { code: 'const el = <Button title={title} />' },
    { code: 'const el = <span aria-label="Ok" />', options: [{ attributes: ['title'] }] },
  ],
  invalid: [
    {
      code: 'const el = <span>Loading...</span>',
      errors: [{ messageId: 'untranslatedText' }],
    },
    {
      code: "const el = <span>{'Loading...'}</span>",
      errors: [{ messageId: 'untranslatedText' }],
    },
    {
      code: 'const el = <span>{`Created ${count} items`}</span>',
      errors: [{ messageId: 'untranslatedText' }],
    },
    {
      code: 'const el = <Button title="Delete" aria-label="Delete entity" />',
      errors: [{ messageId: 'untranslatedText' }, { messageId: 'untranslatedText' }],
    },
    {
      code: 'const el = <Input placeholder={`Search for ${humanName}`} />',
      errors: [{ messageId: 'untranslatedText' }],
    },
    {
      code: 'const el = <span>Lädt …</span>',
      errors: [{ messageId: 'untranslatedText' }],
    },
  ],
});
