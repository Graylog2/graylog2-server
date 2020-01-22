import Reflux from 'reflux';
import URLUtils from 'util/URLUtils';
import UserNotification from 'util/UserNotification';
import fetch from 'logic/rest/FetchProvider';

import StoreProvider from 'injection/StoreProvider';

const NodesStore = StoreProvider.getStore('Nodes');
const SystemProcessingStore = StoreProvider.getStore('SystemProcessing');
const SystemLoadBalancerStore = StoreProvider.getStore('SystemLoadBalancer');

const ClusterOverviewStore = Reflux.createStore({
  sourceUrl: '/cluster',
  clusterOverview: undefined,

  init() {
    this.cluster();
    this.listenTo(SystemProcessingStore, this.cluster);
    this.listenTo(SystemLoadBalancerStore, this.cluster);
    this.listenTo(NodesStore, this.cluster);
  },

  getInitialState() {
    return { clusterOverview: this.clusterOverview };
  },

  cluster() {
    const promise = fetch('GET', URLUtils.qualifyUrl(this.sourceUrl));
    promise.then(
      (response) => {
        this.clusterOverview = response;
        this.trigger({ clusterOverview: this.clusterOverview });
      },
      error => UserNotification.error(`Getting cluster overview failed: ${error}`, 'Could not get cluster overview'),
    );

    return promise;
  },

  threadDump(nodeId) {
    const promise = fetch('GET', URLUtils.qualifyUrl(`${this.sourceUrl}/${nodeId}/threaddump`))
      .then(
        (response) => {
          return response.threaddump;
        },
        error => UserNotification.error(`Getting thread dump for node '${nodeId}' failed: ${error}`, 'Could not get thread dump'),
      );

    return promise;
  },

  processbufferDump(nodeId) {
    const promise = fetch('GET', URLUtils.qualifyUrl(`${this.sourceUrl}/${nodeId}/processbufferdump`))
      .then(
        (response) => {
          return response.processbuffer_dump;
        },
        error => UserNotification.error(`Getting process buffer dump for node '${nodeId}' failed: ${error}`, 'Could not get process buffer dump'),
      );

    return promise;
  },

  jvm(nodeId) {
    const promise = fetch('GET', URLUtils.qualifyUrl(`${this.sourceUrl}/${nodeId}/jvm`));
    promise.catch(error => UserNotification.error(`Getting JVM information for node '${nodeId}' failed: ${error}`, 'Could not get JVM information'));

    return promise;
  },
});

export default ClusterOverviewStore;
