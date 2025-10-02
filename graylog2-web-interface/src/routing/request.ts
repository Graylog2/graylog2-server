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

type Query = { [key: string]: string | number | boolean | string[] };
type Headers = { [key: string]: string | number | boolean | string[] };
type Methods = 'GET' | 'POST' | 'PUT' | 'DELETE';
export type RequestOptions = {
  requestShouldExtendSession?: boolean;
};

const headersFromOptions = (options: RequestOptions): Headers => {
  if (options?.requestShouldExtendSession) {
    return { 'X-Graylog-No-Session-Extension': !(options?.requestShouldExtendSession ?? true) };
  }

  return {};
};
const request = (
  method: Methods,
  path: string,
  body: any,
  query: Query,
  headers: Headers,
  options: RequestOptions = {},
) => {
  const pathWithQueryParameters =
    Object.entries(query).length > 0 ? `${path}?${Qs.stringify(query, { indices: false })}` : path;

  const optionHeaders = headersFromOptions(options);
  const _headers: Headers = {
    ...headers,
    ...optionHeaders,
  };

  return new Builder(method, URLUtils.qualifyUrl(pathWithQueryParameters)).json(body).setHeaders(_headers).build();
};

export default request;
