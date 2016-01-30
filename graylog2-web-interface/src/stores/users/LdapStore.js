import Reflux from 'reflux';

import URLUtils from 'util/URLUtils';
import UserNotification from 'util/UserNotification';
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

  _ldapTest(payload) {
    const url = URLUtils.qualifyUrl(`${this.sourceUrl}/test`);
    return fetch('POST', url, payload);
  },

  testServerConnection(newLdapSettings) {
    const payload = {
      test_connect_only: true,
      ldap_uri: newLdapSettings.ldap_uri,
      system_username: newLdapSettings.system_username,
      system_password: newLdapSettings.system_password,
      use_start_tls: newLdapSettings.use_start_tls,
      trust_all_certificates: newLdapSettings.trust_all_certificates,
      active_directory: newLdapSettings.active_directory,
    };

    return this._ldapTest(payload);
  },

  testLogin(newLdapSettings, principal, password) {
    const payload = {
      test_connect_only: false,
      principal: principal,
      password: password,
      ldap_uri: newLdapSettings.ldap_uri,
      system_username: newLdapSettings.system_username,
      system_password: newLdapSettings.system_password,
      use_start_tls: newLdapSettings.use_start_tls,
      trust_all_certificates: newLdapSettings.trust_all_certificates,
      active_directory: newLdapSettings.active_directory,
      search_base: newLdapSettings.search_base,
      search_pattern: newLdapSettings.search_pattern,
      group_search_base: newLdapSettings.group_search_base,
      group_id_attribute: newLdapSettings.group_id_attribute,
      group_search_pattern: newLdapSettings.group_search_pattern,
    };

    return this._ldapTest(payload);
  },
});

export default LdapStore;
