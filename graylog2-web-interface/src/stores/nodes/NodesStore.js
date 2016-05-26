import Reflux from 'reflux';
import URLUtils from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';

import ActionsProvider from 'injection/ActionsProvider';
const NodesActions = ActionsProvider.getActions('Nodes');

const NodesStore = Reflux.createStore({
  listenables: [NodesActions],
  sourceUrl: '/system/cluster',
  nodes: undefined,
  clusterId: undefined,
  nodeCount: 0,
  INTERVAL: 5000, // 5 seconds

  init() {
    if (this.nodes === undefined) {
      NodesActions.list();
      setInterval(NodesActions.list, this.INTERVAL);
    }
  },

  getInitialState() {
    return this.getNodesInfo();
  },

  getNodesInfo() {
    return { nodes: this.nodes, clusterId: this.clusterId, nodeCount: this.nodeCount };
  },

  list() {
    const promise = fetch('GET', URLUtils.qualifyUrl(URLUtils.concatURLPath(this.sourceUrl, 'nodes')))
      .then(response => {
        this.nodes = {};
        response.nodes.forEach((node) => {
          this.nodes[node.node_id] = node;
        });
        this.clusterId = this._clusterId();
        this.nodeCount = this._nodeCount();
        this._propagateState();
      });

    NodesActions.list.promise(promise);
  },

  getNode(nodeId) {
    return this.nodes[nodeId];
  },

  _clusterId() {
    const nodeInCluster = Object.keys(this.nodes).map(id => this.nodes[id]).find(node => node.cluster_id);
    return (nodeInCluster ? nodeInCluster.cluster_id.toUpperCase() : undefined);
  },

  _nodeCount() {
    return Object.keys(this.nodes).length;
  },

  _propagateState() {
    this.trigger(this.getNodesInfo());
  },
});

export default NodesStore;
