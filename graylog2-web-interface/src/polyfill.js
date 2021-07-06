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

// When adding a polyfill it is probably necessary to extend the polyfill list in the eslint-config-graylog package.
// Otherwise ESLint does not know about the polyfill and throws a warning because of the compat/compat rule.

import 'core-js';
import 'regenerator-runtime/runtime';

// To support IE11 (remove if support is dropped)
import 'promise-polyfill/src/polyfill';
import 'whatwg-fetch';
import 'intersection-observer';
