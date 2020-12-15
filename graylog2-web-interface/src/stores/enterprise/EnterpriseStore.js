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
import CombinedProvider from 'injection/CombinedProvider';

const { EnterpriseActions } = CombinedProvider.get('Enterprise');

const EnterpriseStore = Reflux.createStore({
  listenables: [EnterpriseActions],
  sourceUrl: '/free-enterprise',
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
    const promise = fetch('GET', this.enterpriseUrl('license/info'));

    promise.then(
      (response) => {
        this.licenseStatus = response.free_license_info.license_status;
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

  requestFreeEnterpriseLicense(formValues) {
    const requestBody = {
      first_name: formValues.firstName,
      last_name: formValues.lastName,
      company: formValues.company,
      email: formValues.email,
      phone: formValues.phone,
    };

    const promise = fetch('POST', this.enterpriseUrl('license'), requestBody);

    promise.then(
      (response) => {
        UserNotification.success('Your free Graylog Enterprise license should be on the way.', 'Success!');
        this.refresh();

        return response;
      },
      (error) => {
        const errorMessage = lodash.get(error, 'additional.body.message', error.message);

        UserNotification.error(`Requesting a free Graylog Enterprise license failed: ${errorMessage}`, 'Error');
      },
    );

    EnterpriseActions.requestFreeEnterpriseLicense.promise(promise);
  },
});

export default EnterpriseStore;
