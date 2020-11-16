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
// @flow strict
import Reflux from 'reflux';

import HTTPHeaderAuthConfigActions from 'actions/authentication/HTTPHeaderAuthConfigActions';
import type { Store } from 'stores/StoreTypes';
import fetch from 'logic/rest/FetchProvider';
import ApiRoutes from 'routing/ApiRoutes';
import { qualifyUrl } from 'util/URLUtils';
import { singletonStore } from 'views/logic/singleton';
import HTTPHeaderAuthConfig, { type HTTPHeaderAuthConfigJSON } from 'logic/authentication/HTTPHeaderAuthConfig';

const HTTPHeaderAuthConfigStore: Store<{}> = singletonStore(
  'HTTPHeaderAuthConfig',
  () => Reflux.createStore({
    listenables: [HTTPHeaderAuthConfigActions],

    load(): Promise<HTTPHeaderAuthConfig> {
      const url = qualifyUrl(ApiRoutes.HTTPHeaderAuthConfigController.load().url);
      const promise = fetch('GET', url).then(HTTPHeaderAuthConfig.fromJSON);

      HTTPHeaderAuthConfigActions.load.promise(promise);

      return promise;
    },

    update(payload: HTTPHeaderAuthConfigJSON): Promise<HTTPHeaderAuthConfig> {
      const url = qualifyUrl(ApiRoutes.HTTPHeaderAuthConfigController.update().url);
      const promise = fetch('PUT', url, payload).then(HTTPHeaderAuthConfig.fromJSON);

      HTTPHeaderAuthConfigActions.update.promise(promise);

      return promise;
    },
  }),
);

export { HTTPHeaderAuthConfigActions, HTTPHeaderAuthConfigStore };
