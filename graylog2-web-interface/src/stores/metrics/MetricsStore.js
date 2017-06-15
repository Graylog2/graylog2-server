import Reflux from 'reflux';

import URLUtils from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';
import fetch, { Builder, fetchPeriodically } from 'logic/rest/FetchProvider';
import TimeHelper from 'util/TimeHelper';

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
  promises: {},

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
      result = result.then(() => promise).then(value => accumulator.push(value), error => accumulator.push(error));
    });

    return result.then(() => accumulator);
  },
  _metricsToFetch(localRegistrations, globalRegistrations) {
    const metricsToFetch = {};

    // First collect all node metric registrations
    Object.keys(localRegistrations)
      .filter(nodeId => Object.keys(localRegistrations[nodeId].length > 0))
      .forEach((nodeId) => {
        Object.keys(localRegistrations[nodeId])
          .filter(metricName => localRegistrations[nodeId][metricName] > 0)
          .forEach((metricName) => {
            metricsToFetch[metricName] = 1;
          });
      });

    // Then collect all global metric registrations
    Object.keys(globalRegistrations)
      .filter(metricName => globalRegistrations[metricName] > 0)
      .forEach((metricName) => {
        metricsToFetch[metricName] = 1;
      });
    return metricsToFetch;
  },
  _buildMetricsFromResponse(response) {
    const metrics = {};
    Object.keys(response)
      .forEach((nodeId) => {
        const nodeMetrics = {};

        if (!response[nodeId]) {
          return;
        }
        response[nodeId].metrics.forEach((metric) => {
          nodeMetrics[metric.full_name] = metric;
        });

        metrics[nodeId] = nodeMetrics;
      });

    return metrics;
  },
  list() {
    if (!SessionStore.isLoggedIn()) {
      return;
    }

    const metricsToFetch = this._metricsToFetch(this.registrations, this.globalRegistrations);
    const url = URLUtils.qualifyUrl(ApiRoutes.ClusterMetricsApiController.multipleAllNodes().url);

    if (!this.promises.list) {
      const promise = fetchPeriodically('POST', url, { metrics: Object.keys(metricsToFetch) })
        .finally(() => delete this.promises.list);

      promise.then((response) => {
        this.metrics = this._buildMetricsFromResponse(response);
        // The metricsUpdatedAt value is used by components to decide if they should be re-rendered
        this.trigger({ metrics: this.metrics, metricsUpdatedAt: TimeHelper.nowInSeconds() });
        return this.metrics;
      });
      this.promises.list = promise;
    }

    MetricsActions.list.promise(this.promises.list);
    return this.promises.list;
  },
  names() {
    if (!this.nodes) {
      console.warn('Node list not yet available, not fetching metrics.');
      return;
    }

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
