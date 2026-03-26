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
import { SystemAuthenticationHTTPHeaderAuthConfig } from '@graylog/server-api';

import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';
import type { HTTPHeaderAuthConfigJSON } from 'logic/authentication/HTTPHeaderAuthConfig';
import HTTPHeaderAuthConfig from 'logic/authentication/HTTPHeaderAuthConfig';
import UserNotification from 'util/UserNotification';
import { defaultOnError } from 'util/conditional/onError';

const load = (): Promise<HTTPHeaderAuthConfig> =>
  defaultOnError(
    SystemAuthenticationHTTPHeaderAuthConfig.getConfig().then(HTTPHeaderAuthConfig.fromJSON),
    'Loading HTTP header authentication config failed',
  );

const update = (payload: HTTPHeaderAuthConfigJSON): Promise<HTTPHeaderAuthConfig> => {
  const url = qualifyUrl(ApiRoutes.HTTPHeaderAuthConfigController.update().url);
  const promise = fetch('PUT', url, payload).then(HTTPHeaderAuthConfig.fromJSON);

  return promise
    .then((result) => {
      UserNotification.success('Successfully updated HTTP header authentication config');

      return result;
    })
    .catch((error) => {
      UserNotification.error(`Updating HTTP header authentication config failed with status: ${error}`);
      throw error;
    });
};

export default {
  load,
  update,
};
