/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
// @flow strict
import Reflux from 'reflux';

import fetch, { fetchPlainText } from 'logic/rest/FetchProvider';
import PaginationURL from 'util/PaginationURL';
import ApiRoutes from 'routing/ApiRoutes';
import { qualifyUrl } from 'util/URLUtils';
import UserNotification from 'util/UserNotification';

type GrokPattern = {
  id: string,
  name: string,
  pattern: string,
  content_pack: string,
};

type PaginatedResponse = {
  count: number,
  total: number,
  page: number,
  per_page: number,
  patterns: Array<GrokPattern>,
};

type GrokPatternTest = {
  name: string,
  pattern: string,
  sampleData: string,
};

const GrokPatternsStore = Reflux.createStore({
  URL: qualifyUrl('/system/grok'),

  loadPatterns(callback: (patterns: Array<GrokPattern>) => void) {
    const failCallback = (error) => {
      UserNotification.error(`Loading Grok patterns failed with status: ${error.message}`,
        'Could not load Grok patterns');
    };

    // get the current list of patterns and sort it by name
    return fetch('GET', this.URL)
      .then(
        (resp: any) => {
          const { patterns } = resp;

          patterns.sort((pattern1: GrokPattern, pattern2: GrokPattern) => {
            return pattern1.name.toLowerCase()
              .localeCompare(pattern2.name.toLowerCase());
          });

          callback(patterns);

          return resp;
        },
        failCallback,
      );
  },

  searchPaginated(page, perPage, query) {
    const url = PaginationURL(ApiRoutes.GrokPatternsController.paginated().url, page, perPage, query);

    return fetch('GET', qualifyUrl(url))
      .then((response: PaginatedResponse) => {
        const pagination = {
          count: response.count,
          total: response.total,
          page: response.page,
          perPage: response.per_page,
          query: query,
        };

        return {
          patterns: response.patterns,
          pagination: pagination,
        };
      })
      .catch((errorThrown) => {
        UserNotification.error(`Loading patterns failed with status: ${errorThrown}`,
          'Could not load streams');
      });
  },

  testPattern(pattern: GrokPatternTest, callback: (request: any) => void, errCallback: (errorMessage: string) => void) {
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

    fetch('POST', qualifyUrl(ApiRoutes.GrokPatternsController.test().url), requestPatternTest)
      .then(
        (response) => {
          callback(response);

          return response;
        },
        failCallback,
      );
  },

  savePattern(pattern: GrokPattern, callback: () => void) {
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

  deletePattern(pattern: GrokPattern, callback: () => void) {
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

  bulkImport(patterns: string, replaceAll: boolean) {
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

    const promise = fetchPlainText('POST', `${this.URL}?replace=${String(replaceAll)}`, patterns);

    promise.catch(failCallback);

    return promise;
  },
});

export default GrokPatternsStore;
