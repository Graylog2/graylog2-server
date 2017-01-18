import ApiRoutes = require('routing/ApiRoutes');

const UserNotification = require('util/UserNotification');
const URLUtils = require('util/URLUtils');
const fetch = require('logic/rest/FetchProvider').default;
const lodash = require('lodash');

interface StreamRuleType {
  id: number;
  short_desc: string;
  long_desc: string;
}

interface StreamRule {
  field: string;
  type: number;
  value: string;
  inverted: boolean;
  description: string;
}

interface Callback {
  (): void;
}

class StreamRulesStore {
  private callbacks: Array<Callback> = [];

  types(callback: ((streamRuleTypes: Array<StreamRuleType>) => void)) {
    var url = "/streams/null/rules/types";
    var promise = fetch('GET', URLUtils.qualifyUrl(url));

    return promise;
  }
  list(streamId: string, callback: ((streamRules: Array<StreamRule>) => void)) {
    var failCallback = (error) => {
      UserNotification.error("Fetching Stream Rules failed with status: " + error,
        "Could not retrieve Stream Rules");
    };

    fetch('GET', URLUtils.qualifyUrl(ApiRoutes.StreamRulesApiController.list(streamId).url)).then(callback, failCallback);
  }
  update(streamId: string, streamRuleId: string, data: StreamRule, callback: (() => void)) {
    var failCallback = (error) => {
      UserNotification.error("Updating Stream Rule failed with status: " + error,
        "Could not update Stream Rule");
    };

    var url = URLUtils.qualifyUrl(ApiRoutes.StreamRulesApiController.update(streamId, streamRuleId).url);
    var request = {field: data.field, type: data.type, value: data.value, inverted: data.inverted, description: data.description};

    fetch('PUT', url, request).then(callback, failCallback).then(this._emitChange.bind(this));
  }
  remove(streamId: string, streamRuleId: string, callback: (() => void)) {
    var failCallback = (error) => {
      UserNotification.error("Deleting Stream Rule failed with status: " + error,
        "Could not delete Stream Rule");
    };

    var url = URLUtils.qualifyUrl(ApiRoutes.StreamRulesApiController.delete(streamId, streamRuleId).url);
    fetch('DELETE', url).then(callback, failCallback).then(this._emitChange.bind(this));
  }
  create(streamId: string, data: StreamRule, callback: (() => void)) {
    var failCallback = (error) => {
      UserNotification.error("Creating Stream Rule failed with status: " + error,
        "Could not create Stream Rule");
    };

    var url = URLUtils.qualifyUrl(ApiRoutes.StreamRulesApiController.create(streamId).url);

    fetch('POST', url, data).then(callback, failCallback).then(this._emitChange.bind(this));
  }
  onChange(callback) {
    this.callbacks.push(callback);
  }
  _emitChange() {
    this.callbacks.forEach((callback) => callback());
  }
  unregister(callback: Callback) {
    lodash.pull(this.callbacks, callback);
  }
}

var streamRulesStore = new StreamRulesStore();

export = streamRulesStore;
