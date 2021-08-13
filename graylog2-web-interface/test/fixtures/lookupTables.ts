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

// eslint-disable-next-line import/prefer-default-export
export const createLookupTable = (index = 1, overrides = {}) => ({
  cache_id: 'cache-id',
  content_pack: null,
  data_adapter_id: 'data-adapter-id',
  default_multi_value: '',
  default_multi_value_type: 'NULL',
  default_single_value: '',
  default_single_value_type: 'NULL',
  description: `Description lookup-table-${index}`,
  id: `lookup-table-id-${index}`,
  name: `lookup-table-name-${index}`,
  title: `Lookup Table Title ${index}`,
  ...overrides,
});
