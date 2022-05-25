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
import lodash from 'lodash';

import { qualifyUrl } from 'util/URLUtils';
import UserNotification from 'util/UserNotification';
import fetch from 'logic/rest/FetchProvider';
import { singletonStore, singletonActions } from 'logic/singleton';

export const EnterpriseActions = singletonActions(
  'core.Enterprise',
  () => Reflux.createActions({
    getLicenseInfo: { asyncResult: true },
  }),
);

export const EnterpriseStore = singletonStore(
  'core.Enterprise',
  () => Reflux.createStore({
    listenables: [EnterpriseActions],
    sourceUrl: '/enterprise/license',
    licenseStatus: undefined,

    getInitialState() {
      return this.getState();
    },

    propagateChanges() {
      this.trigger(this.getState());
    },

    getState() {
      return {
        licenseStatus: this.licenseStatus,
      };
    },

    enterpriseUrl(path = '') {
      return qualifyUrl(`${this.sourceUrl}/${path}`);
    },

    refresh() {
      this.getLicenseInfo();
    },

    getLicenseInfo() {
      const promise = fetch('GET', this.enterpriseUrl('info'));

      promise.then(
        (response) => {
          this.licenseStatus = response.license_info.license_status;
          this.propagateChanges();

          return response;
        },
        (error) => {
          const errorMessage = lodash.get(error, 'additional.body.message', error.message);

          UserNotification.error(`Couldn't load license information: ${errorMessage}`, 'Error');
        },
      );

      EnterpriseActions.getLicenseInfo.promise(promise);
    },
  }),
);
