import Reflux from 'reflux';
import URLUtils from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';

import ActionsProvider from 'injection/ActionsProvider';
const SingleNodeActions = ActionsProvider.getActions('SingleNode');

const SingleNodeStore = Reflux.createStore({
  listenables: [SingleNodeActions],
  sourceUrl: '/system/cluster/node',
  node: undefined,

  init() {
    this._propagateState();
  },

  getInitialState() {
    return this._getNodeInfo();
  },

  _getNodeInfo() {
    return { node: this.node };
  },

  _propagateState() {
    this.trigger(this._getNodeInfo());
  },

  get(nodeId) {
    const promise = fetch('GET', URLUtils.qualifyUrl(nodeId ? URLUtils.concatURLPath(this.sourceUrl, nodeId) : this.sourceUrl))
      .then((response) => {
        this.node = response;
        this._propagateState();
      });

    SingleNodeActions.get.promise(promise);
  },
});

export default SingleNodeStore;
