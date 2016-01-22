import Reflux from 'reflux';

import RulesActions from 'RulesActions';

import UserNotification from 'util/UserNotification';
import URLUtils from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';

const urlPrefix = '/plugins/org.graylog.plugins.pipelineprocessor';

const RulesStore = Reflux.createStore({
  listenables: [RulesActions],
  rules: undefined,

  list() {
    const failCallback = (error) => {
      UserNotification.error('Fetching rules failed with status: ' + error.message,
        'Could not retrieve processing rules');
    };

    const url = URLUtils.qualifyUrl(urlPrefix + '/system/pipelines/rule');
    return fetch('GET', url).then((response) => {
      this.rules = response;
      this.trigger({rules: response});
    }, failCallback);
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

export default RulesStore;