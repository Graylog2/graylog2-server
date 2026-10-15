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
import i18n from 'i18n';

/**
 * Translates texts of visualization bindings. Bindings are created when the module is loaded, so their texts are
 * getters which translate at the time they are displayed, e.g. `get displayName() { return visT('vis_types.bar'); }`.
 */
export const visT = (key: string, options: { [name: string]: unknown } = {}) =>
  i18n.t(key, { ns: 'aggregation', ...options });

/** Select options whose values are shown as labels, e.g. `['linear', 'logarithmic']`, with translated labels. */
export const translatedOptions = (values: ReadonlyArray<string>, keyPrefix: string): Array<[string, string]> =>
  values.map((value) => [visT(`${keyPrefix}.${value}`), value]);
