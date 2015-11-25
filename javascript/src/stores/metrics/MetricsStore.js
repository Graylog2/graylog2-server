import Reflux from 'reflux';

import UserNotification from 'util/UserNotification';
import URLUtils from 'util/URLUtils';
import jsRoutes from 'routing/jsRoutes';
import fetch from 'logic/rest/FetchProvider';

import MetricsActions from 'actions/metrics/MetricsActions';

const MetricsStore = Reflux.createStore({
  listenables: [MetricsActions],
  namespace: 'org.graylog2',
  registrations: {},
  metrics: {},

  init() {
    MetricsActions.names();
  },
  getInitialState() {
    return { names: this.names, metrics: this.metrics };
  },
  list() {
    const url = URLUtils.qualifyUrl(jsRoutes.controllers.api.MetricsApiController.multiple().url);
    const body = { metrics: Object.keys(this.registrations).filter((metricName) => this.registrations[metricName] > 0) };
    const promise = fetch('POST', url, body).then((response) => {
      const metrics = {};
      response.metrics.forEach((metric) => metrics[metric.full_name] = metric);
      this.metrics = metrics;
      this.trigger({ metrics: metrics });
      return metrics;
    });

    MetricsActions.list.promise(promise);
  },
  names() {
    const url = URLUtils.qualifyUrl(jsRoutes.controllers.api.MetricsApiController.byNamespace(this.namespace).url);
    const promise = fetch('GET', url).then((response) => {
      this.trigger({ names: response.metrics });
      this.names = response.metrics;

      return response.metrics;
    });

    MetricsActions.names.promise(promise);
  },
  add(metricName) {
    this.registrations[metricName] = this.registrations[metricName] ? this.registrations[metricName] + 1 : 1;
  },
  remove(metricName) {
    this.registrations[metricName] = this.registrations[metricName] > 0 ? this.registrations[metricName] - 1 : 0;
  },
});

export default MetricsStore;
