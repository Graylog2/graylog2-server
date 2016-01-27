import Reflux from 'reflux';

import PipelinesActions from 'PipelinesActions';

import UserNotification from 'util/UserNotification';
import URLUtils from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';

const urlPrefix = '/plugins/org.graylog.plugins.pipelineprocessor';

const PipelinesStore = Reflux.createStore({
  listenables: [PipelinesActions],
  pipelines: undefined,

  list() {
    const failCallback = (error) => {
      UserNotification.error('Fetching pipelines failed with status: ' + error.message,
        'Could not retrieve processing pipelines');
    };

    const url = URLUtils.qualifyUrl(urlPrefix + '/system/pipelines/pipeline');
    return fetch('GET', url).then((response) => {
      this.pipelines = response;
      this.trigger({pipelines: response});
    }, failCallback);
  },

  get(pipelineId) {

  },

  save(pipelineSource) {
    const failCallback = (error) => {
      UserNotification.error('Saving pipeline failed with status: ' + error.message,
        'Could not save processing pipeline');
    };
    const url = URLUtils.qualifyUrl(urlPrefix + '/system/pipelines/pipeline');
    const pipeline = {
      title: pipelineSource.title,
      description: pipelineSource.description,
      source: pipelineSource.source
    };
    return fetch('POST', url, pipeline).then((response) => {
      this.pipelines = response;
      this.trigger({pipelines: response});
    }, failCallback);
  },

  update(pipelineSource) {
    const failCallback = (error) => {
      UserNotification.error('Updating pipeline failed with status: ' + error.message,
        'Could not update processing pipeline');
    };
    const url = URLUtils.qualifyUrl(urlPrefix + '/system/pipelines/pipeline/' + pipelineSource.id);
    const pipeline = {
      id: pipelineSource.id,
      title: pipelineSource.title,
      description: pipelineSource.description,
      source: pipelineSource.source
    };
    return fetch('PUT', url, pipeline).then((response) => {
      this.pipelines = this.pipelines.map((e) => e.id === response.id ? response : e);
      this.trigger({pipelines: this.pipelines});
    }, failCallback);
  },
  delete(pipelineId) {
    const failCallback = (error) => {
      UserNotification.error('Updating pipeline failed with status: ' + error.message,
        'Could not update processing pipeline');
    };
    const url = URLUtils.qualifyUrl(urlPrefix + '/system/pipelines/pipeline/' + pipelineId);
    return fetch('DELETE', url).then(() => {
      this.pipelines = this.pipelines.filter((el) => el.id !== pipelineId);
      this.trigger({pipelines: this.pipelines});
    }, failCallback);
  },
  parse(pipelineSource, callback) {
    const url = URLUtils.qualifyUrl(urlPrefix + '/system/pipelines/pipeline/parse');
    const pipeline = {
      title: pipelineSource.title,
      description: pipelineSource.description,
      source: pipelineSource.source
    };
    return fetch('POST', url, pipeline).then(
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

export default PipelinesStore;