import Reflux from 'reflux';

const LdapActions = Reflux.createActions({
  loadSettings: { asyncResult: true },
  update: { asyncResult: true },
  testServerConnection: { asyncResult: true },
  testLogin: { asyncResult: true },
});

export default LdapActions;
