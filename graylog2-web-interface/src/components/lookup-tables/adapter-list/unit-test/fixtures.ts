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
const iterable = Object.keys([...new Array(10)]);

// eslint-disable-next-line import/prefer-default-export
export const DATA_ADAPTERS = iterable.map((item: string) => ({
  id: `${item}-data-adapter-id`,
  _scope: 'DEFAULT',
  title: `${item} adapter title`,
  description: `${item} adapter description`,
  name: `${item} adapter name`,
  custom_error_ttl_enabled: false,
  custom_error_ttl: null,
  custom_error_ttl_unit: null,
  content_pack: null,
  config: {
    type: 'torexitnode',
  },
}));

export const ERROR_STATE = {
  data_adapters: Object.fromEntries(
    DATA_ADAPTERS.map(({ name }: { name: string }, i: number) => [name, i === 1 ? 'Adapter test error' : null]),
  ),
  tables: [],
  caches: [],
};
