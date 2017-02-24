import Reflux from 'reflux';
import URLUtils from 'util/URLUtils';
import UserNotification from 'util/UserNotification';

import fetch from 'logic/rest/FetchProvider';

import ActionsProvider from 'injection/ActionsProvider';
const LdapGroupsActions = ActionsProvider.getActions('LdapGroups');

const LdapGroupsStore = Reflux.createStore({
  listenables: [LdapGroupsActions],
  sourceUrl: '/system/ldap',
  groups: undefined,
  mapping: undefined,

  getInitialState() {
    return { groups: this.groups, mapping: this.mapping };
  },

  loadGroups() {
    const url = URLUtils.qualifyUrl(`${this.sourceUrl}/groups`);

    const promise = fetch('GET', url);
    promise.then(
      (response) => {
        this.groups = response;
        this.trigger({ groups: this.groups });
      },
      (error) => {
        if (error.additional.status !== 400) {
          UserNotification.error(`Loading LDAP group list failed with status: ${error}`,
            'Could not load LDAP group list');
        }
      },
    );

    LdapGroupsActions.loadGroups.promise(promise);
  },

  loadMapping() {
    const url = URLUtils.qualifyUrl(`${this.sourceUrl}/settings/groups`);

    const promise = fetch('GET', url);
    promise.then(
      (response) => {
        this.mapping = response;
        this.trigger({ mapping: this.mapping });
      },
      (error) => {
        UserNotification.error(`Loading LDAP group mapping failed with status: ${error}`,
          'Could not load LDAP group mapping');
      },
    );

    LdapGroupsActions.loadMapping.promise(promise);
  },

  saveMapping(mapping) {
    const url = URLUtils.qualifyUrl(`${this.sourceUrl}/settings/groups`);

    const promise = fetch('PUT', url, mapping);
    promise.then(
      () => {
        this.loadMapping();
        UserNotification.success('LDAP group mapping successfully updated.');
      },
      (error) => {
        UserNotification.error(`Updating LDAP group mapping failed with status: ${error}`,
          'Could not update LDAP group mapping');
      },
    );

    LdapGroupsActions.saveMapping.promise(promise);
  },
});

export default LdapGroupsStore;
