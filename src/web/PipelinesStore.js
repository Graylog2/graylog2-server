import Reflux from 'reflux';

import PipelinesActions from 'PipelinesActions';

import UserNotification from 'util/UserNotification';
import URLUtils from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';

const urlPrefix = '/plugins/org.graylog.plugins.pipelineprocessor';

const PipelinesStore = Reflux.createStore({
  listenables: [PipelinesActions],
  pipelines: undefined,

  init() {
    this.list().then((pipelines) => {
      this.pipelines = pipelines;
      this.trigger({pipelines: pipelines});
    });
  },

  getInitialState() {
    return {
      pipelines: this.pipelines,
    }
  },

  list() {
    const failCallback = (error) => {
      UserNotification.error('Fetching pipelines failed with status: ' + error.message,
        'Could not retrieve processing pipelines');
    };

    const url = URLUtils.qualifyUrl(urlPrefix + '/system/pipelines/pipeline');
    const promise = fetch('GET', url).then((response) => {
      return response.pipelines;
    }, failCallback);

    PipelinesActions.list.promise(promise);

    return promise;
 },

  get(pipelineId) {

  },

  save(pipelineSource) {

  },

  update(pipelineId) {

  },
  delete(pipelineId) {

  },
});

export default PipelinesStore;