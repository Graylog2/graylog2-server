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
/* eslint-disable no-console */
import { format } from 'util';
import { DEPRECATION_NOTICE } from 'util/deprecationNotice';

console.origWarn = console.warn;
console.origError = console.error;

const ignoredWarnings = [
  'react-async-component-lifecycle-hooks',
  'react-unsafe-component-lifecycles',
  DEPRECATION_NOTICE,
];

const ignoreWarning = (args) => (!args[0] || ignoredWarnings.filter((warning) => args[0].includes(warning)).length > 0);

console.warn = jest.fn((...args) => {
  console.origWarn(...args);
  if (!ignoreWarning(args)) {
    throw new Error(format(...args));
  }
});

console.error = jest.fn((...args) => {
  console.origError(...args);
  if (!ignoreWarning(args)) {
    throw new Error(format(...args));
  }
});
