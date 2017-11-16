import Reflux from 'reflux';
import URLUtils from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';

import ActionsProvider from 'injection/ActionsProvider';

const ClusterTrafficActions = ActionsProvider.getActions('ClusterTraffic');

const ClusterTrafficStore = Reflux.createStore({
  listenables: ClusterTrafficActions,

  traffic() {
    const promise = fetch('GET', URLUtils.qualifyUrl('/system/cluster/traffic'));
    promise.then((response) => {
      this.trigger(response);
    });
    return promise;
  },

});

export default ClusterTrafficStore;
