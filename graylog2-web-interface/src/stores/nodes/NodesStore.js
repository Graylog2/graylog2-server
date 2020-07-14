// @flow strict
import Reflux from 'reflux';

import { qualifyUrl } from 'util/URLUtils';
import { fetchPeriodically } from 'logic/rest/FetchProvider';
import ApiRoutes from 'routing/ApiRoutes';
import CombinedProvider from 'injection/CombinedProvider';

const { NodesActions } = CombinedProvider.get('Nodes');
const { SessionStore } = CombinedProvider.get('Session');

type NodeInfo = {
  cluster_id: string,
  hostname: string,
  is_master: boolean,
  last_seen: string,
  node_id: string,
  short_node_id: string,
  transport_address: string,
  type: 'server',
};

type NodesListResponse = {
  nodes: ?Array<NodeInfo>,
  total: number,
};

const NodesStore = Reflux.createStore({
  listenables: [NodesActions],
  nodes: undefined,
  clusterId: undefined,
  nodeCount: 0,
  INTERVAL: 5000, // 5 seconds
  promises: {},

  init() {
    if (this.nodes === undefined) {
      this._triggerList();
      setInterval(this._triggerList, this.INTERVAL);
    }
  },

  _triggerList() {
    if (SessionStore.isLoggedIn()) {
      NodesActions.list();
    }
  },

  getInitialState() {
    return this.getNodesInfo();
  },

  getNodesInfo() {
    return { nodes: this.nodes, clusterId: this.clusterId, nodeCount: this.nodeCount };
  },

  list() {
    const promise = this.promises.list || fetchPeriodically('GET', qualifyUrl(ApiRoutes.ClusterApiResource.list().url))
      .then((response: NodesListResponse) => {
        this.nodes = {};

        if (response.nodes) {
          this.nodes = response.nodes
            .map((node) => [node.node_id, node])
            .reduce((prev, [key, value]) => ({ ...prev, [key]: value }), {});

          this.clusterId = this._clusterId();
          this.nodeCount = this._nodeCount();
          this._propagateState();
        }

        return response;
      })
      .finally(() => delete this.promises.list);

    this.promises.list = promise;

    NodesActions.list.promise(promise);
  },

  getNode(nodeId) {
    return this.nodes[nodeId];
  },

  _clusterId() {
    const nodeInCluster = Object.keys(this.nodes).map((id) => this.nodes[id]).find((node) => node.cluster_id);

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
