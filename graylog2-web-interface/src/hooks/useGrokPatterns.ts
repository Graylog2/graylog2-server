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
import { SystemGrok } from '@graylog/server-api';

import { fetchPlainText } from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import UserNotification from 'util/UserNotification';

export type GrokPattern = {
  id: string;
  name: string;
  pattern: string;
  content_pack: string;
};

type GrokPatternTest = {
  name: string;
  pattern: string;
  sampleData: string;
};

export const loadGrokPatterns = (callback: (patterns: Array<GrokPattern>) => void) => {
  const failCallback = (error) => {
    UserNotification.error(
      `Loading Grok patterns failed with status: ${error.message}`,
      'Could not load Grok patterns',
    );
  };

  return (SystemGrok.listGrokPatterns() as Promise<{ patterns: Array<GrokPattern> }>).then((resp) => {
    const { patterns } = resp;

    patterns.sort((pattern1, pattern2) => pattern1.name.toLowerCase().localeCompare(pattern2.name.toLowerCase()));

    callback(patterns);

    return resp;
  }, failCallback);
};

export const searchGrokPatternsPaginated = (page: number, perPage: number, query: string) =>
  (SystemGrok.getPage('name', page, perPage, query) as Promise<any>)
    .then((response) => ({
      patterns: response.patterns,
      pagination: {
        count: response.count,
        total: response.total,
        page: response.page,
        perPage: response.per_page,
        query: query,
      },
    }))
    .catch((errorThrown) => {
      UserNotification.error(`Loading patterns failed with status: ${errorThrown}`, 'Could not load streams');
    });

export const testGrokPattern = (
  pattern: GrokPatternTest,
  callback: (request: any) => void,
  errCallback: (errorMessage: string) => void,
) => {
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

  (SystemGrok.testPattern(requestPatternTest as any) as Promise<any>).then((response) => {
    callback(response);

    return response;
  }, failCallback);
};

export const saveGrokPattern = (pattern: GrokPattern, callback: () => void) => {
  const failCallback = (error) => {
    let errorMessage = error.message;
    const errorBody = error.additional.body;

    if (errorBody && errorBody.message) {
      errorMessage = error.additional.body.message;
    }

    UserNotification.error(
      `Saving Grok pattern "${pattern.name}" failed with status: ${errorMessage}`,
      'Could not save Grok pattern',
    );
  };

  const requestPattern = {
    id: pattern.id,
    pattern: pattern.pattern,
    name: pattern.name,
    content_pack: pattern.content_pack,
  };

  const promise =
    pattern.id === ''
      ? SystemGrok.createPattern(requestPattern as any)
      : SystemGrok.updatePattern(pattern.id, requestPattern as any);

  (promise as Promise<any>).then((response) => {
    callback();
    const action = pattern.id === '' ? 'created' : 'updated';
    const message = `Grok pattern "${pattern.name}" successfully ${action}`;

    UserNotification.success(message);

    return response;
  }, failCallback);
};

export const deleteGrokPattern = (pattern: GrokPattern, callback: () => void) => {
  const failCallback = (error) => {
    UserNotification.error(
      `Deleting Grok pattern "${pattern.name}" failed with status: ${error.message}`,
      'Could not delete Grok pattern',
    );
  };

  SystemGrok.removePattern(pattern.id).then((response) => {
    callback();
    UserNotification.success(`Grok pattern "${pattern.name}" successfully deleted`);

    return response;
  }, failCallback);
};

export const bulkImportGrokPatterns = (patterns: string, importStrategy: string) => {
  const failCallback = (error) => {
    let errorMessage = error.message;
    const errorBody = error.additional?.body;

    if (errorBody && errorBody.validation_errors && errorBody.validation_errors._) {
      errorMessage = '';
      const errors = errorBody.validation_errors._;

      errors.forEach((err) => {
        errorMessage = errorMessage.concat(err.error);
      });
    }

    UserNotification.error(
      `Importing Grok pattern file failed with status: ${errorMessage}`,
      'Could not load Grok patterns',
    );
  };

  const promise = fetchPlainText('POST', qualifyUrl(`/system/grok?import-strategy=${importStrategy}`), patterns);

  promise.catch(failCallback);

  return promise;
};
