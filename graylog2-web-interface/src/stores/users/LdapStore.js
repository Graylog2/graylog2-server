import Reflux from 'reflux';

import URLUtils from 'util/URLUtils';
import UserNotification from 'util/UserNotification';
import jsRoutes from 'routing/jsRoutes';
import fetch from 'logic/rest/FetchProvider';

const LdapStore = Reflux.createStore({
  sourceUrl: '/system/ldap/',
  ldapSettings: undefined,

  init() {
    this.loadSettings();
  },

  getInitialState() {
    return {ldapSettings: this.ldapSettings};
  },

  loadSettings() {
    const url = URLUtils.qualifyUrl(`${this.sourceUrl}/settings`);

    const promise = fetch('GET', url);
    promise.then(response => {
      this.ldapSettings = response;
      this.trigger({ldapSettings: response});
    });

    return promise;
  },

  update(newLdapSettings) {
    const url = URLUtils.qualifyUrl(`${this.sourceUrl}/settings`);

    const promise = fetch('PUT', url, newLdapSettings);
    promise.then(
      () => UserNotification.success('LDAP settings saved successfully'),
      error => UserNotification.error(`Saving LDAP settings failed: ${error}`, 'Could not save LDAP settings')
    );

    return promise;
  },

  testServerConnection(newLdapSettings) {
    const url = URLUtils.qualifyUrl(`${this.sourceUrl}/test`);
    const testPayload = {
      test_connect_only: true,
      ldap_uri: newLdapSettings.ldap_uri,
      system_username: newLdapSettings.system_username,
      system_password: newLdapSettings.system_password,
      use_start_tls: newLdapSettings.use_start_tls,
      trust_all_certificates: newLdapSettings.trust_all_certificates,
      active_directory: newLdapSettings.active_directory,
    };

    return fetch('POST', url, testPayload);
  },
});

export default LdapStore;
