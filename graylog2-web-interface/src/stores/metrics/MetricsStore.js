import Reflux from 'reflux';

import UserNotification from 'util/UserNotification';
import URLUtils from 'util/URLUtils';
import jsRoutes from 'routing/jsRoutes';
import fetch from 'logic/rest/FetchProvider';

import NodesStore from 'stores/nodes/NodesStore';

import MetricsActions from 'actions/metrics/MetricsActions';
import SessionActions from 'actions/sessions/SessionActions';

const MetricsStore = Reflux.createStore({
  listenables: [MetricsActions, SessionActions],
  namespace: 'org.graylog2',
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
    MetricsActions.names();
  },
  logout() {
    this.registrations = {};
    this.globalRegistrations = {};
  },
  list() {
    const metricsToFetch = {};
    Object.keys(this.registrations)
      .filter((nodeId) => Object.keys(this.registrations[nodeId]).length > 0)
      .forEach(nodeId => {
        metricsToFetch[nodeId] = Object.keys(this.registrations[nodeId]).filter(metricName => this.registrations[nodeId][metricName] > 0);
      });
    const globalMetrics = Object.keys(this.globalRegistrations).filter(metricName => this.globalRegistrations[metricName] > 0);

    if (this.nodes) {
      Object.keys(this.nodes).forEach(nodeId => {
        globalMetrics.forEach(metricName => {
          if (!metricsToFetch[nodeId]) {
            metricsToFetch[nodeId] = [];
          }
          if (metricsToFetch[nodeId].indexOf(metricName) == -1) {
            metricsToFetch[nodeId].push(metricName);
          }
        });
      });
    }

    const promises = Object.keys(metricsToFetch)
      .map((nodeId) => {
        const url = URLUtils.qualifyUrl(jsRoutes.controllers.api.ClusterMetricsApiController.multiple(nodeId).url);
        const body = {metrics: metricsToFetch[nodeId]};
        return fetch('POST', url, body).then((response) => {
          const metrics = {};
          response.metrics.forEach((metric) => metrics[metric.full_name] = metric);
          return {nodeId: nodeId, metrics: metrics};
        });
      });

    const promise = Promise.all(promises).then((responses) => {
      const metrics = {};
      responses.forEach((response) => {
        metrics[response.nodeId] = response.metrics;
      });

      this.trigger({metrics: metrics});
      this.metrics = metrics;
      return metrics;
    });

    MetricsActions.list.promise(promise);
  },
  names() {
    const promise = Promise.all(Object.keys(this.nodes).map((nodeId) => {
      const url = URLUtils.qualifyUrl(jsRoutes.controllers.api.ClusterMetricsApiController.byNamespace(nodeId, this.namespace).url);
      return fetch('GET', url).then((response) => {
        return {nodeId: nodeId, names: response.metrics};
      });
    })).then((responses) => {
      const metricsNames = {};
      responses.forEach((response) => {
        metricsNames[response.nodeId] = response.names;
      });
      this.trigger({metricsNames: metricsNames});
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
