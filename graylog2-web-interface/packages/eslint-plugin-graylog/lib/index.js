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

const pluginName = 'graylog';
const graylogPlugin = {
  meta: { name: pluginName },
  rules: {
    'prefer-hook': require('./rules/prefer-hook'),
    'use-arrow-function-for-event-handlers-in-classes': require('./rules/use-arrow-function-for-event-handlers-in-classes'),
  },
};

const configs = {
  recommended: {
    plugins: { [pluginName]: graylogPlugin },
    rules: {
      [`${pluginName}/prefer-hook`]: 'warn',
      [`${pluginName}/use-arrow-function-for-event-handlers-in-classes`]: 'error',
    },
  },
};

module.exports = {
  ...graylogPlugin,
  configs,
};
