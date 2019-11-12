import Reflux from 'reflux';

import fetch, { fetchPlainText } from 'logic/rest/FetchProvider';
import ApiRoutes from 'routing/ApiRoutes';
import URLUtils from 'util/URLUtils';
import UserNotification from 'util/UserNotification';

const GrokPatternsStore = Reflux.createStore({
  URL: URLUtils.qualifyUrl('/system/grok'),

  loadPatterns(callback) {
    const failCallback = (error) => {
      UserNotification.error(`Loading Grok patterns failed with status: ${error.message}`,
        'Could not load Grok patterns');
    };
    // get the current list of patterns and sort it by name
    return fetch('GET', this.URL)
      .then(
        (resp) => {
          const { patterns } = resp;
          patterns.sort((pattern1, pattern2) => {
            return pattern1.name.toLowerCase()
              .localeCompare(pattern2.name.toLowerCase());
          });
          callback(patterns);
          return resp;
        },
        failCallback,
      );
  },

  testPattern(pattern, callback, errCallback) {
    const failCallback = (error) => {
      let errorMessage = error.message;
      const errorBody = error.additional.body;
      if (errorBody && errorBody.message) {
        errorMessage = error.additional.body.message;
      }
      errCallback(errorMessage);
    };

    const requestPatternTest = {
      grok_pattern: {
        name: pattern.name,
        pattern: pattern.pattern,
      },
      sampleData: pattern.sampleData,
    };

    fetch('POST', URLUtils.qualifyUrl(ApiRoutes.GrokPatternsController.test().url), requestPatternTest)
      .then(
        (response) => {
          callback(response);
          return response;
        },
        failCallback,
      );
  },

  savePattern(pattern, callback) {
    const failCallback = (error) => {
      let errorMessage = error.message;
      const errorBody = error.additional.body;
      if (errorBody && errorBody.message) {
        errorMessage = error.additional.body.message;
      }
      UserNotification.error(`Testing Grok pattern "${pattern.name}" failed with status: ${errorMessage}`,
        'Could not test Grok pattern');
    };

    const requestPattern = {
      id: pattern.id,
      pattern: pattern.pattern,
      name: pattern.name,
      content_pack: pattern.content_pack,
    };

    let url = this.URL;
    let method;
    if (pattern.id === '') {
      method = 'POST';
    } else {
      url += `/${pattern.id}`;
      method = 'PUT';
    }
    fetch(method, url, requestPattern)
      .then(
        (response) => {
          callback();
          const action = pattern.id === '' ? 'created' : 'updated';
          const message = `Grok pattern "${pattern.name}" successfully ${action}`;
          UserNotification.success(message);
          return response;
        },
        failCallback,
      );
  },

  deletePattern(pattern, callback) {
    const failCallback = (error) => {
      UserNotification.error(`Deleting Grok pattern "${pattern.name}" failed with status: ${error.message}`,
        'Could not delete Grok pattern');
    };
    fetch('DELETE', `${this.URL}/${pattern.id}`)
      .then(
        (response) => {
          callback();
          UserNotification.success(`Grok pattern "${pattern.name}" successfully deleted`);
          return response;
        },
        failCallback,
      );
  },

  bulkImport(patterns, replaceAll) {
    const failCallback = (error) => {
      let errorMessage = error.message;
      const errorBody = error.additional.body;
      if (errorBody && errorBody.validation_errors && errorBody.validation_errors._) {
        errorMessage = '';
        const errors = errorBody.validation_errors._;
        // eslint-disable-next-line no-plusplus
        for (let i = 0, len = errors.length; i < len; i++) {
          errorMessage = errorMessage.concat(errors[i].error);
        }
      }
      UserNotification.error(`Importing Grok pattern file failed with status: ${errorMessage}`,
        'Could not load Grok patterns');
    };

    const promise = fetchPlainText('POST', `${this.URL}?replace=${replaceAll}`, patterns);

    promise.catch(failCallback);

    return promise;
  },
});

export default GrokPatternsStore;
