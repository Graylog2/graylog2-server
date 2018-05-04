const UserNotification = require("util/UserNotification");
const URLUtils = require('util/URLUtils');
const ApiRoutes = require('routing/ApiRoutes');

const fetchDefault = require('logic/rest/FetchProvider').default;
const fetchPlainText = require('logic/rest/FetchProvider').fetchPlainText;

interface GrokPattern {
  id: string;
  name: string;
  pattern: string;
}

interface GrokPatternTest {
  name: string,
  pattern: string,
  sampleData: string,
}

const GrokPatternsStore = {
  URL: URLUtils.qualifyUrl('/system/grok'),

  loadPatterns(callback: (patterns: Array<GrokPattern>) => void) {
    var failCallback = (error) => {
      UserNotification.error("Loading Grok patterns failed with status: " + error.message,
        "Could not load Grok patterns");
    };
    // get the current list of patterns and sort it by name
    return fetchDefault('GET', this.URL).then(
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

  testPattern(pattern: GrokPatternTest, callback: (response) => void, errCallback: (err_message) => void) {
    const failCallback = (error) => {
      let err_message = error.message;
      let err_body = error.additional.body;
      if (err_body && err_body.message) {
          err_message = error.additional.body.message;
      }
      errCallback(err_message);
    };

    const requestPatternTest = {
      grok_pattern: {
        name: pattern.name,
        pattern: pattern.pattern
      },
      sampleData: pattern.sampleData
    };

    fetchDefault('POST', URLUtils.qualifyUrl(ApiRoutes.GrokPatternsController.test().url), requestPatternTest)
      .then(
        response => {
          callback(response);
          return response;
        },
        failCallback
      );

  },

  savePattern(pattern: GrokPattern, callback: () => void) {
    const failCallback = (error) => {
      let err_message = error.message;
      let err_body = error.additional.body;
      if (err_body && err_body.message) {
        err_message = error.additional.body.message;
      }
      UserNotification.error(`Testing Grok pattern "${pattern.name}" failed with status: ${err_message}`,
        "Could not test Grok pattern");
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
      let err_message = error.message;
      let err_body = error.additional.body;
      if (err_body && err_body.validation_errors && err_body.validation_errors._) {
        err_message = "";
        const errors = err_body.validation_errors._;
        for(let i = 0, len = errors.length; i < len; i++) {
          err_message = err_message.concat(errors[i].error);
        }
      }
      UserNotification.error("Importing Grok pattern file failed with status: " + err_message,
        "Could not load Grok patterns");
    };

    const promise = fetchPlainText('POST', `${this.URL}?replace=${replaceAll}`, patterns);

    promise.catch(failCallback);

    return promise;
  },
};

module.exports = GrokPatternsStore;
