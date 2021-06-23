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
import Reflux from 'reflux';
import Promise from 'bluebird';

import * as URLUtils from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';
import fetch from 'logic/rest/FetchProvider';

const SystemStore = Reflux.createStore({
  system: undefined,
  locales: undefined,
  init() {
    this.info().then((response) => {
      this.trigger({ system: response });
      this.system = response;
    });

    this.systemLocales().then((response) => {
      this.trigger({ locales: response });
      this.locales = response.locales;
    });
  },
  getInitialState() {
    return { system: this.system, locales: this.locales };
  },
  info() {
    const url = URLUtils.qualifyUrl(ApiRoutes.SystemApiController.info().url);

    return fetch('GET', url);
  },
  jvm() {
    const url = URLUtils.qualifyUrl(ApiRoutes.SystemApiController.jvm().url);

    return fetch('GET', url);
  },
  systemLocales() {
    const url = URLUtils.qualifyUrl(ApiRoutes.SystemApiController.locales().url);

    return fetch('GET', url);
  },
  elasticsearchVersion() {
    const url = URLUtils.qualifyUrl(ApiRoutes.ClusterApiResource.elasticsearchStats().url);

    const promise = new Promise((resolve, reject) => {
      fetch('GET', url).then((response) => {
        const splitVersion = response.cluster_version.split('.');

        resolve({ major: splitVersion[0], minor: splitVersion[1], patch: splitVersion[2] });
      }).catch(reject);
    });

    return promise;
  },
});

export default SystemStore;
