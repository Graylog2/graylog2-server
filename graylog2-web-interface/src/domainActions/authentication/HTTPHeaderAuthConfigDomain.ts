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

import i18n from 'i18n';
import type { HTTPHeaderAuthConfigJSON } from 'logic/authentication/HTTPHeaderAuthConfig';
import HTTPHeaderAuthConfig from 'logic/authentication/HTTPHeaderAuthConfig';
import UserNotification from 'util/UserNotification';
import { defaultOnError } from 'util/conditional/onError';

const load = (): Promise<HTTPHeaderAuthConfig> =>
  defaultOnError(
    SystemAuthenticationHTTPHeaderAuthConfig.getConfig().then(HTTPHeaderAuthConfig.fromJSON),
    i18n.t('http_header_notifications.load_failed', { ns: 'authentication' }),
  );

const update = (payload: HTTPHeaderAuthConfigJSON): Promise<HTTPHeaderAuthConfig> => {
  const promise = SystemAuthenticationHTTPHeaderAuthConfig.updateConfig(payload).then(HTTPHeaderAuthConfig.fromJSON);

  return promise
    .then((result) => {
      UserNotification.success(i18n.t('http_header_notifications.update_success', { ns: 'authentication' }));

      return result;
    })
    .catch((error) => {
      UserNotification.error(i18n.t('http_header_notifications.update_failed', { ns: 'authentication', error }));
      throw error;
    });
};

export default {
  load,
  update,
};
