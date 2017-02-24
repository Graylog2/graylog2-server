import Reflux from 'reflux';

const LdapGroupsActions = Reflux.createActions({
  loadGroups: { asyncResult: true },
  loadMapping: { asyncResult: true },
  saveMapping: { asyncResult: true },
});

export default LdapGroupsActions;
