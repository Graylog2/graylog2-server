/// <reference path="../../../declarations/bluebird/bluebird.d.ts" />

import jsRoutes = require('routing/jsRoutes');
const fetch = require('logic/rest/FetchProvider').default;
const UserNotification = require('util/UserNotification');
const URLUtils = require('util/URLUtils');

const LdapGroupsStore = {
  loadGroups(): Promise<string[]> {
    const promise = fetch('GET', URLUtils.qualifyUrl(jsRoutes.controllers.LdapController.apiGroups().url));
    promise.catch(error => {
      if (error.additional.status !== 404) {
        UserNotification.error(`Loading LDAP group list failed with status: ${error}`,
          'Could not load LDAP group list');
      }
    });

    return promise;
  },
  loadMapping(): Promise<Object> {
    const promise = fetch('GET', URLUtils.qualifyUrl(jsRoutes.controllers.LdapController.apiGroupsMapping().url));
    promise.catch(error => {
      if (error.additional.status !== 404) {
        UserNotification.error(`Loading LDAP group mapping failed with status: ${error}`,
          'Could not load LDAP group mapping');
      }
    });

    return promise;
  },
  saveMapping(mapping) {
    const url = URLUtils.qualifyUrl(jsRoutes.controllers.LdapController.apiGroupsMapping().url);
    const promise = fetch('PUT', url, mapping);
    promise.then(
      () => UserNotification.success('LDAP group mapping successfully updated.'),
      error => {
        if (error.additional.status !== 404) {
          UserNotification.error(`Updating LDAP group mapping failed with status: ${error}`,
            'Could not update LDAP group mapping');
        }
      });

    return promise;
  }
};

export default LdapGroupsStore;
