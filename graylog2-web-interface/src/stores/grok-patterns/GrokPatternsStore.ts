const UserNotification = require("util/UserNotification");
const URLUtils = require('util/URLUtils');

const fetchDefault = require('logic/rest/FetchProvider').default;
const fetchPlainText = require('logic/rest/FetchProvider').fetchPlainText;

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
    fetchDefault('GET', this.URL).then(
      (resp: any) => {
        const patterns = resp.patterns;
        patterns.sort((pattern1: GrokPattern, pattern2: GrokPattern) => {
          return pattern1.name.toLowerCase().localeCompare(pattern2.name.toLowerCase());
        });
        callback(patterns);
        return resp;
      },
      failCallback);
  },

  savePattern(pattern: GrokPattern, callback: () => void) {
    var failCallback = (error) => {
      console.log(error.additional.body.message);
      let err_message = error.message;
      if (error.additional.body && error.additional.body.message) {
        err_message = error.additional.body.message.replace(/\n/g, "<br/>").replace(/ /g, "&nbsp;");
        err_message = `<br/><code>${err_message}</code>`;
      }
      UserNotification.error("Saving Grok pattern \"" + pattern.name + "\" failed with status: " + err_message,
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
    fetchDefault(method, url, requestPattern)
      .then(
        response => {
          callback();
          const action = pattern.id === "" ? "created" : "updated";
          const message = "Grok pattern \"" + pattern.name + "\" successfully " + action;
          UserNotification.success(message);
          return response;
        },
        failCallback
      );
  },

  deletePattern(pattern: GrokPattern, callback: () => void) {
    var failCallback = (error) => {
      UserNotification.error("Deleting Grok pattern \"" + pattern.name + "\" failed with status: " + error.message,
        "Could not delete Grok pattern");
    };
    fetchDefault('DELETE', this.URL + "/" + pattern.id)
      .then(
        response => {
          callback();
          UserNotification.success("Grok pattern \"" + pattern.name + "\" successfully deleted");
          return response;
        },
        failCallback
      );
  },

  bulkImport(patterns: string, replaceAll: boolean) {
    var failCallback = (error) => {
      UserNotification.error("Importing Grok pattern file failed with status: " + error.message,
        "Could not load Grok patterns");
    };

    const promise = fetchPlainText('POST', `${this.URL}?replace=${replaceAll}`, patterns);

    promise.catch(failCallback);

    return promise;
  },
};

module.exports = GrokPatternsStore;
