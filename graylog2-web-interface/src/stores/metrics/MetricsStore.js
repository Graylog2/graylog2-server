import Reflux from 'reflux';

import URLUtils from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';
import fetch from 'logic/rest/FetchProvider';

import StoreProvider from 'injection/StoreProvider';
const SessionStore = StoreProvider.getStore('Session');
const NodesStore = StoreProvider.getStore('Nodes');

import ActionsProvider from 'injection/ActionsProvider';
const SessionActions = ActionsProvider.getActions('Session');
const MetricsActions = ActionsProvider.getActions('Metrics');

const MetricsStore = Reflux.createStore({
  listenables: [MetricsActions, SessionActions],
  namespace: 'org',
  registrations: {},
  globalRegistrations: {},

  init() {
    this.listenTo(NodesStore, this.updateNodes);
  },
  getInitialState() {
    return { metricsNames: this.metricsNames, metrics: this.metrics };
  },
  updateNodes(update) {
    this.nodes = update.nodes;
  },
  _allResults(promises) {
    const accumulator = [];
    let result = Promise.resolve(null);

    promises.forEach((promise) => {
      result = result.then(() => promise).then((value) => accumulator.push(value), (error) => accumulator.push(error));
    });

    return result.then(() => accumulator);
  },
  list() {
    if (!SessionStore.isLoggedIn()) {
      return;
    }
    const metricsToFetch = {};

    // First collect all node metric registrations
    Object.keys(this.registrations)
      .filter((nodeId) => Object.keys(this.registrations[nodeId].length > 0))
      .forEach((nodeId) => {
        Object.keys(this.registrations[nodeId])
          .filter((metricName) => this.registrations[nodeId][metricName] > 0)
          .forEach((metricName) => {
            metricsToFetch[metricName] = 1;
          });
      });

    // Then collect all global metric registrations
    Object.keys(this.globalRegistrations)
      .filter((metricName) => this.globalRegistrations[metricName] > 0)
      .forEach((metricName) => {
        metricsToFetch[metricName] = 1;
      });

    const url = URLUtils.qualifyUrl(ApiRoutes.ClusterMetricsApiController.multipleAllNodes().url);
    const promise = fetch('POST', url, { metrics: Object.keys(metricsToFetch) });

    promise.then((response) => {
      const metrics = {};
      Object.keys(response)
        .forEach((nodeId) => {
          const nodeMetrics = {};

          response[nodeId].metrics.forEach((metric) => {
            nodeMetrics[metric.full_name] = metric;
          });

          metrics[nodeId] = nodeMetrics;
        });
      this.trigger({ metrics: metrics });
      this.metrics = metrics;
      return metrics;
    });

    MetricsActions.list.promise(promise);
  },
  names() {
    const promise = this._allResults(Object.keys(this.nodes).map((nodeId) => {
      const url = URLUtils.qualifyUrl(ApiRoutes.ClusterMetricsApiController.byNamespace(nodeId, this.namespace).url);
      return fetch('GET', url).then((response) => {
        return { nodeId: nodeId, names: response.metrics };
      });
    })).then((responses) => {
      const metricsNames = {};
      responses.forEach((response) => {
        if (response.nodeId) {
          metricsNames[response.nodeId] = response.names;
        }
      });
      this.trigger({ metricsNames: metricsNames });
      this.metricsNames = metricsNames;
      return metricsNames;
    });

    MetricsActions.names.promise(promise);
  },
  add(nodeId, metricName) {
    if (!this.registrations[nodeId]) {
      this.registrations[nodeId] = {};
    }
    this.registrations[nodeId][metricName] = this.registrations[nodeId][metricName] ? this.registrations[nodeId][metricName] + 1 : 1;
  },
  addGlobal(metricName) {
    if (!this.globalRegistrations[metricName]) {
      this.globalRegistrations[metricName] = 1;
    } else {
      this.globalRegistrations[metricName] += 1;
    }
  },
  remove(nodeId, metricName) {
    if (!this.registrations[nodeId]) {
      return;
    }
    this.registrations[nodeId][metricName] = this.registrations[nodeId][metricName] > 0 ? this.registrations[nodeId][metricName] - 1 : 0;
    if (this.registrations[nodeId][metricName] === 0) {
      delete this.registrations[nodeId][metricName];
    }
  },
  removeGlobal(metricName) {
    if (!this.globalRegistrations[metricName]) {
      return;
    }
    this.globalRegistrations[metricName] = this.globalRegistrations[metricName] > 0 ? this.globalRegistrations[metricName] - 1 : 0;
    if (this.globalRegistrations[metricName] === 0) {
      delete this.globalRegistrations[metricName];
    }
  },
});

export default MetricsStore;
