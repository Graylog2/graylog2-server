import Reflux from 'reflux';

import URLUtils from 'util/URLUtils';
import jsRoutes from 'routing/jsRoutes';
import fetch from 'logic/rest/FetchProvider';

const LdapStore = Reflux.createStore({
  ldapSettings: undefined,
  init() {
    this.trigger({ldapSettings: this.ldapSettings});
  },

  getInitialState() {
    return {ldapSettings: this.ldapSettings};
  },
});

export default LdapStore;
