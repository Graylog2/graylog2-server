const UserNotification = require('util/UserNotification');
const URLUtils = require('util/URLUtils');
import ApiRoutes = require('routing/ApiRoutes');
const fetch = require('logic/rest/FetchProvider').default;

interface Output {
  id: string;
  title: string;
  type: string;
}

const OutputsStore = {
  OUTPUTS_URL: URLUtils.qualifyUrl(ApiRoutes.OutputsApiController.index().url),

  load(callback : (outputs: Array<Output>) => void) {
    fetch('GET', this.OUTPUTS_URL).then(callback, this._failCallback);
  },
  loadForStreamId(streamId: string, callback: (outputs: Array<Output>) => void) {
    const url = URLUtils.qualifyUrl(ApiRoutes.StreamOutputsApiController.index(streamId).url);
    fetch('GET', url).then(callback, this._failCallback);
  },
  loadAvailableTypes(callback: (available: any) => void) {
    const url = URLUtils.qualifyUrl(ApiRoutes.OutputsApiController.availableTypes().url);
    fetch('GET', url).then(callback, this._failCallback);
  },
  loadAvailable(typeName: string, callback: (available: any) => void) {
    const url = URLUtils.qualifyUrl(ApiRoutes.OutputsApiController.availableTypes().url);
    fetch('GET', url).then((resp) => {
      return resp.types[typeName];
    }, this._failCallback).then(callback);
  },
  remove(outputId: string, callback: (error) => void) {
    const url = URLUtils.qualifyUrl(ApiRoutes.OutputsApiController.delete(outputId).url);
    fetch('DELETE', url).then(callback, (error) => {
      UserNotification.error("Terminating output failed with status: " + error,
        "Could not terminate output");
    });
  },
  save(output: any, callback: (output: Output) => void) {
    const failCallback = (error) => {
      UserNotification.error("Saving Output \"" + output.title + "\" failed with status: " + error,
        "Could not save Output");
    };

    const url = URLUtils.qualifyUrl(ApiRoutes.OutputsApiController.create().url);

    fetch('POST', url, output).then(callback, failCallback);
  },
  update(output: Output, deltas: any, callback: (output: Output) => void) {
    const failCallback = (error) => {
      UserNotification.error("Updating Output \"" + output.title + "\" failed with status: " + error,
        "Could not update Output");
    };

    const url = URLUtils.qualifyUrl(ApiRoutes.OutputsApiController.update(output.id).url);

    fetch('PUT', url, deltas).then(callback, failCallback);
  },
  _failCallback(error: string) {
    UserNotification.error("Loading outputs failed with status: " + error,
      "Could not load outputs");
  }
};

export = OutputsStore;
