import Reflux from 'reflux';

import fetch from 'logic/rest/FetchProvider';
import ApiRoutes from 'routing/ApiRoutes';
import URLUtils from 'util/URLUtils';
import UserNotification from 'util/UserNotification';

const OutputsStore = Reflux.createStore({
  OUTPUTS_URL: URLUtils.qualifyUrl(ApiRoutes.OutputsApiController.index().url),

  load(callback) {
    fetch('GET', this.OUTPUTS_URL)
      .then(callback, this._failCallback);
  },
  loadForStreamId(streamId, callback) {
    const url = URLUtils.qualifyUrl(ApiRoutes.StreamOutputsApiController.index(streamId).url);
    fetch('GET', url)
      .then(callback, this._failCallback);
  },
  loadAvailableTypes(callback) {
    const url = URLUtils.qualifyUrl(ApiRoutes.OutputsApiController.availableTypes().url);
    fetch('GET', url)
      .then(callback, this._failCallback);
  },
  loadAvailable(typeName, callback) {
    const url = URLUtils.qualifyUrl(ApiRoutes.OutputsApiController.availableTypes().url);
    fetch('GET', url)
      .then((resp) => {
        return resp.types[typeName];
      }, this._failCallback)
      .then(callback);
  },
  remove(outputId, callback) {
    const url = URLUtils.qualifyUrl(ApiRoutes.OutputsApiController.delete(outputId).url);
    fetch('DELETE', url)
      .then(callback, (error) => {
        UserNotification.error(`Terminating output failed with status: ${error}`,
          'Could not terminate output');
      });
  },
  save(output, callback) {
    const failCallback = (error) => {
      UserNotification.error(`Saving Output "${output.title}" failed with status: ${error}`,
        'Could not save Output');
    };

    const url = URLUtils.qualifyUrl(ApiRoutes.OutputsApiController.create().url);

    fetch('POST', url, output)
      .then(callback, failCallback);
  },
  update(output, deltas, callback) {
    const failCallback = (error) => {
      UserNotification.error(`Updating Output "${output.title}" failed with status: ${error}`,
        'Could not update Output');
    };

    const url = URLUtils.qualifyUrl(ApiRoutes.OutputsApiController.update(output.id).url);

    fetch('PUT', url, deltas)
      .then(callback, failCallback);
  },
  _failCallback(error) {
    UserNotification.error(`Loading outputs failed with status: ${error}`,
      'Could not load outputs');
  },
});

export default OutputsStore;
