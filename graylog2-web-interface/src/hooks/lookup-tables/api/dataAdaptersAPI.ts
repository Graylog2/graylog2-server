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

import fetch from 'logic/rest/FetchProvider';
import URLUtils from 'util/URLUtils';
import type { LookupTableAdapter } from 'logic/lookup-tables/types';

const getURL = (path: string = '') => (URLUtils.qualifyUrl(`/system/lookup${path}`));

// eslint-disable-next-line import/prefer-default-export
export const fetchAll = async (page = 1, perPage = 100, query = null) => {
  let url = getURL(`/adapters?sort=title&order=asc&page=${page}&per_page=${perPage}`);
  if (query) url += `&query=${encodeURI(query)}`;

  return fetch('GET', url);
};

export const fetchErrors = async (dataAdapters: LookupTableAdapter[]) => {
  const dataAdapterNames = dataAdapters.map((adapter: LookupTableAdapter) => adapter.name);

  return fetch('POST', getURL('/errorstates'), { data_adapters: dataAdapterNames });
};
