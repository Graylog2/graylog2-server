const UserNotification = require("util/UserNotification");
const URLUtils = require('util/URLUtils');

const fetch = require('logic/rest/FetchProvider').default;

interface GrokPattern {
  id: string;
  name: string;
  pattern: string;
}

const GrokPatternsStore = {
  URL: URLUtils.qualifyUrl('/system/grok'),

  loadPatterns(callback: (patterns: Array<GrokPattern>) => void) {
    var failCallback = (error) => {
      UserNotification.error("Loading Grok patterns failed with status: " + error.message,
        "Could not load Grok patterns");
    };
    // get the current list of patterns and sort it by name
    fetch('GET', this.URL).then((resp: any) => {
      const patterns = resp.patterns;
      patterns.sort((pattern1: GrokPattern, pattern2: GrokPattern) => {
        return pattern1.name.toLowerCase().localeCompare(pattern2.name.toLowerCase());
      });
      callback(patterns);
    }, failCallback);
  },

  savePattern(pattern: GrokPattern, callback: () => void) {
    var failCallback = (error) => {
      UserNotification.error("Saving Grok pattern \"" + pattern.name + "\" failed with status: " + error.message,
        "Could not save Grok pattern");
    };

    const requestPattern = {
      id: pattern.id,
      pattern: pattern.pattern,
      name: pattern.name,
      'content_pack': pattern['content_pack'],
    };

    let url = this.URL;
    let method;
    if (pattern.id === "") {
      method = 'POST';
    } else {
      url += '/' + pattern.id;
      method = 'PUT';
    }
    fetch(method, url, requestPattern).then(() => {
      callback();
      var action = pattern.id === "" ? "created" : "updated";
      var message = "Grok pattern \"" + pattern.name + "\" successfully " + action;
      UserNotification.success(message);
    }).catch(failCallback);
  },

  deletePattern(pattern: GrokPattern, callback: () => void) {
    var failCallback = (error) => {
      UserNotification.error("Deleting Grok pattern \"" + pattern.name + "\" failed with status: " + error.message,
        "Could not delete Grok pattern");
    };
    fetch('DELETE', this.URL + "/" + pattern.id).then(() => {
      callback();
      UserNotification.success("Grok pattern \"" + pattern.name + "\" successfully deleted");
    }).catch(failCallback);
  },

  bulkImport(patterns: string[], replaceAll: boolean) {
    var failCallback = (error) => {
      UserNotification.error("Importing Grok pattern file failed with status: " + error.message,
        "Could not load Grok patterns");
    };

    const promise = fetch('PUT', `${this.URL}?replace=${replaceAll}`, {patterns: patterns});

    promise.catch(failCallback);

    return promise;
  },
};

module.exports = GrokPatternsStore;
