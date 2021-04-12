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
  extends: [
    'stylelint-config-standard',
    'stylelint-config-styled-components',
  ],
  rules: {
    'declaration-block-trailing-semicolon': 'always',
    'declaration-colon-newline-after': null,
    'declaration-colon-space-after': 'always',
    'declaration-empty-line-before': null,
    'function-name-case': null,
    'function-whitespace-after': null,
    'max-empty-lines': 2,
    'no-descending-specificity': null,
    'no-empty-source': null,
    'no-eol-whitespace': [
      true, {
        ignore: ['empty-lines'],
      },
    ],
    'no-missing-end-of-source-newline': null,
    'property-no-vendor-prefix': [true, {
      ignoreProperties: ['grid-rows', 'grid-columns', 'grid-row', 'grid-column'],
    }],
    'value-no-vendor-prefix': [true, {
      ignoreValues: ['grid', 'inline-grid'],
    }],
    'value-keyword-case': null,
  },
};
