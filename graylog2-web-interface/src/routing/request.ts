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
import Qs from 'qs';

import { Builder } from 'logic/rest/FetchProvider';
import * as URLUtils from 'util/URLUtils';

const request = (method: string, path: string, body: any, query: { [key: string]: string | number | boolean }, headers: { [key: string]: any }) => {
  const pathWithQueryParameters = Object.entries(query).length > 0 ? `${path}?${Qs.stringify(query)}` : path;
  const builder = new Builder(method, URLUtils.qualifyUrl(pathWithQueryParameters))
    .setHeader('X-Graylog-No-Session-Extension', 'true');

  const builderWithHeaders = Object.entries(headers)
    .reduce((prev, [key, value]) => builder.setHeader(key, value), builder);

  return builderWithHeaders.build();
};

export default request;
