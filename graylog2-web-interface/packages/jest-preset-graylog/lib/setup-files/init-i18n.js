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
/* eslint-disable @typescript-eslint/no-require-imports,no-undef,no-console */
// Registers the core translations with their bundled English texts, like `index.tsx` does in the browser.
// Without it, components using `useTranslation` render translation keys instead of texts.
require('locales/registerCoreTranslations');

// Translation keys can't be type-checked, because i18next's key types require `strictNullChecks`.
// Instead, using a key which does not exist logs an error, which fails the test.
const { default: i18n } = require('i18n');

i18n.options.saveMissing = true;
i18n.options.missingKeyHandler = (_languages, namespace, key) => {
  console.error(`Missing translation for key "${key}" in namespace "${namespace}".`);
};
