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
import i18n from 'i18n';

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
      i18n.t('use_grok_patterns_notifications.load_failed', { ns: 'grokpatterns', error: error.message }),
      i18n.t('use_grok_patterns_notifications.load_failed_title', { ns: 'grokpatterns' }),
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
      UserNotification.error(i18n.t('use_grok_patterns_notifications.load_streams_failed', { ns: 'grokpatterns', error: errorThrown }), i18n.t('use_grok_patterns_notifications.load_streams_failed_title', { ns: 'grokpatterns' }));
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
      i18n.t('use_grok_patterns_notifications.save_failed', { ns: 'grokpatterns', name: pattern.name, error: errorMessage }),
      i18n.t('use_grok_patterns_notifications.save_failed_title', { ns: 'grokpatterns' }),
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
    const message =
      pattern.id === ''
        ? i18n.t('use_grok_patterns_notifications.created_success', { ns: 'grokpatterns', name: pattern.name })
        : i18n.t('use_grok_patterns_notifications.updated_success', { ns: 'grokpatterns', name: pattern.name });

    UserNotification.success(message);

    return response;
  }, failCallback);
};

export const deleteGrokPattern = (pattern: GrokPattern, callback: () => void) => {
  const failCallback = (error) => {
    UserNotification.error(
      i18n.t('use_grok_patterns_notifications.delete_failed', { ns: 'grokpatterns', name: pattern.name, error: error.message }),
      i18n.t('use_grok_patterns_notifications.delete_failed_title', { ns: 'grokpatterns' }),
    );
  };

  SystemGrok.removePattern(pattern.id).then((response) => {
    callback();
    UserNotification.success(i18n.t('use_grok_patterns_notifications.deleted_success', { ns: 'grokpatterns', name: pattern.name }));

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
      i18n.t('use_grok_patterns_notifications.import_failed', { ns: 'grokpatterns', error: errorMessage }),
      i18n.t('use_grok_patterns_notifications.load_failed_title', { ns: 'grokpatterns' }),
    );
  };

  const promise = fetchPlainText('POST', qualifyUrl(`/system/grok?import-strategy=${importStrategy}`), patterns);

  promise.catch(failCallback);

  return promise;
};
