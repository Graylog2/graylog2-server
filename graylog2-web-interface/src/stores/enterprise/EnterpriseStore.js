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
  licenseInstalled: undefined,

  getInitialState() {
    return this.getState();
  },

  propagateChanges() {
    this.trigger(this.getState());
  },

  getState() {
    return {
      licenseInstalled: this.licenseInstalled,
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
    promise.catch((error) => {
      const errorMessage = lodash.get(error, 'additional.body.message', error.message);
      UserNotification.error(`Couldn't load license information: ${errorMessage}`, 'Error');
    });
    EnterpriseActions.getLicenseInfo.promise(promise);
  },

  requestFreeEnterpriseLicense(clusterId, formValues) {
    const requestBody = {
      cluster_id: clusterId,
      first_name: formValues.firstName,
      last_name: formValues.lastName,
      company: formValues.company,
      email: formValues.email,
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
