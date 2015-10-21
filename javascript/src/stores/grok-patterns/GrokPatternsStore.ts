import UserNotification = require("util/UserNotification");
import URLUtils = require("util/URLUtils");

const fetch = require('logic/rest/FetchProvider').default;

interface GrokPattern {
  id: string;
  name: string;
  pattern: string;
}

const GrokPatternsStore = {
  URL: URLUtils.qualifyUrl('/system/grok'),

  loadPatterns(callback: (patterns: Array<GrokPattern>) => void) {
    var failCallback = (jqXHR, textStatus, errorThrown) => {
      UserNotification.error("Loading Grok patterns failed with status: " + errorThrown,
        "Could not load Grok patterns");
    };
    // get the current list of patterns and sort it by name
    fetch('GET', this.URL).then((patterns: Array<GrokPattern>) => {
      patterns.sort((pattern1: GrokPattern, pattern2: GrokPattern) => {
        return pattern1.name.toLowerCase().localeCompare(pattern2.name.toLowerCase());
      });
      callback(patterns);
    }).catch(failCallback);
  },

  savePattern(pattern: GrokPattern, callback: () => void) {
    var failCallback = (jqXHR, textStatus, errorThrown) => {
      UserNotification.error("Saving Grok pattern \"" + pattern.name + "\" failed with status: " + errorThrown,
        "Could not save Grok pattern");
    };

    var url;
    if (pattern.id === "") {
      url = this.URL + '/create';
    } else {
      url = this.URL + '/update';
    }
    fetch('POST', url, pattern).then(() => {
      callback();
      var action = pattern.id === "" ? "created" : "updated";
      var message = "Grok pattern \"" + pattern.name + "\" successfully " + action;
      UserNotification.success(message);
    }).catch(failCallback);
  },

  deletePattern(pattern: GrokPattern, callback: () => void) {
    var failCallback = (jqXHR, textStatus, errorThrown) => {
      UserNotification.error("Deleting Grok pattern \"" + pattern.name + "\" failed with status: " + errorThrown,
        "Could not delete Grok pattern");
    };
    fetch('DELETE', this.URL + "/" + pattern.id).then(() => {
      callback();
      UserNotification.success("Grok pattern \"" + pattern.name + "\" successfully deleted");
    }).catch(failCallback);
  }
};

export default GrokPatternsStore;