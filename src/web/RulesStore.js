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

  get(ruleId) {

  },

  save(ruleSource) {
    const failCallback = (error) => {
      UserNotification.error('Saving rule failed with status: ' + error.message,
        'Could not save processing rule');
    };
    const url = URLUtils.qualifyUrl(urlPrefix + '/system/pipelines/rule');
    const rule = {
      title: ruleSource.title,
      description: ruleSource.description,
      source: ruleSource.source
    };
    return fetch('POST', url, rule).then((response) => {
      this.rules = response;
      this.trigger({rules: response});
    }, failCallback);
  },

  update(ruleSource) {
    const failCallback = (error) => {
      UserNotification.error('Updating rule failed with status: ' + error.message,
        'Could not update processing rule');
    };
    const url = URLUtils.qualifyUrl(urlPrefix + '/system/pipelines/rule/' + ruleSource.id);
    const rule = {
      id: ruleSource.id,
      title: ruleSource.title,
      description: ruleSource.description,
      source: ruleSource.source
    };
    return fetch('PUT', url, rule).then((response) => {
      this.rules = this.rules.map((e) => e.id === response.id ? response : e);
      this.trigger({rules: this.rules});
    }, failCallback);
  },
  delete(ruleId) {
    const failCallback = (error) => {
      UserNotification.error('Updating rule failed with status: ' + error.message,
        'Could not update processing rule');
    };
    const url = URLUtils.qualifyUrl(urlPrefix + '/system/pipelines/rule/' + ruleId);
    return fetch('DELETE', url).then(() => {
      this.rules = this.rules.filter((el) => el.id !== ruleId);
      this.trigger({rules: this.rules});
    }, failCallback);
  },
  parse(ruleSource, callback) {
    const url = URLUtils.qualifyUrl(urlPrefix + '/system/pipelines/rule/parse');
    const rule = {
      title: ruleSource.title,
      description: ruleSource.description,
      source: ruleSource.source
    };
    return fetch('POST', url, rule).then(
      (response) => {
        // call to clear the errors, the parsing was successful
        callback([]);
      },
      (error) => {
        // a Bad Request indicates a parse error, set all the returned errors in the editor
        const response = error.additional.res;
        if (response.status === 400) {
          callback(response.body);
        }
      }
    );
  }
});

export default RulesStore;