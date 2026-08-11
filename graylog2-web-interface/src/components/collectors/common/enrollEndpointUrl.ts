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
import URI from 'urijs';

import AppConfig from 'util/AppConfig';
import { qualifyUrl } from 'util/URLUtils';

/**
 * The URL a collector enrolls against: the admin-configured hostname from the collectors config,
 * plus the scheme and port of the Graylog API (e.g. http://…:9000 in dev — taken from the
 * qualified server URL, which resolves a relative `gl2ServerUrl` against the current location),
 * and the app path prefix appended when Graylog is served under a subpath. The collector figures
 * out the API path itself, so the prefix is the app prefix, not the API path.
 */
const enrollEndpointUrl = (hostname: string): string => {
  const apiUrl = new URI(qualifyUrl(''));
  const scheme = apiUrl.protocol() || 'https';
  const port = apiUrl.port();
  const pathPrefix = (AppConfig.gl2AppPathPrefix() ?? '').replace(/^\/+|\/+$/g, '');

  let url = new URI(`${scheme}://${hostname}`);
  if (port) url = url.port(port);
  if (pathPrefix) url = url.path(pathPrefix);

  return url.toString().replace(/\/+$/, '');
};

export default enrollEndpointUrl;
