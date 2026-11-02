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
import { useQuery } from '@tanstack/react-query';

import { Extractors } from '@graylog/server-api';

import ExtractorUtils from 'util/ExtractorUtils';
import UserNotification from 'util/UserNotification';
import i18n from 'i18n';

export const EXTRACTORS_QUERY_KEY = ['extractors'] as const;

export type InputSummary = {
  creator_user_id: string;
  node: string;
  name: string;
  created_at: string;
  global: boolean;
  attributes: {
    [_key: string]: object;
  };
  id: string;
  title: string;
  type: string;
  content_pack: string;
  static_fields: {
    [_key: string]: string;
  };
};

export type NodeSummary = {
  cluster_id: string;
  hostname: string;
  is_leader: boolean;
  is_master: boolean;
  last_seen: string;
  node_id: string;
  short_node_id: string;
  transport_address: string;
  type: string;
};

type RateMetricsResponse = {
  total: number;
  mean: number;
  five_minute: number;
  fifteen_minute: number;
  one_minute: number;
};

type TimerMetricsResponse = {
  min: number;
  max: number;
  std_dev: number;
  mean: number;
  '95th_percentile': number;
  '99th_percentile': number;
  '98th_percentile': number;
};

type TimerRateMetricsResponse = {
  rate: RateMetricsResponse;
  rate_unit: string;
  time: TimerMetricsResponse;
  duration_unit: string;
};

export type ExtractorMetrics = {
  condition_misses: number;
  execution: TimerRateMetricsResponse;
  total: TimerRateMetricsResponse;
  condition: TimerRateMetricsResponse;
  condition_hits: number;
  converters: TimerRateMetricsResponse;
};

export type ExtractorType = {
  creator_user_id: string;
  extractor_type?: string;
  source_field: string;
  condition_type: string;
  converter_exceptions: number;
  title: string;
  type: string;
  cursor_strategy: string;
  exceptions: number;
  target_field: string;
  extractor_config: {
    [_key: string]: object;
  };
  condition_value: string;
  converters: {
    [_key: string]: object;
  }[];
  id?: string;
  metrics: ExtractorMetrics;
  order: number;
};

const getExtractorDTO = (extractor: ExtractorType) => {
  const conditionValue =
    extractor.condition_type && extractor.condition_type !== 'none' ? extractor.condition_value : '';

  return {
    title: extractor.title,
    cursor_strategy: extractor.cursor_strategy || 'copy',
    source_field: extractor.source_field,
    target_field: extractor.target_field,
    extractor_type: extractor.type || extractor.extractor_type, // "extractor_type" needed for imports
    extractor_config: extractor.extractor_config,
    converters: extractor.converters,
    condition_type: extractor.condition_type || 'none',
    condition_value: conditionValue,
    order: extractor.order,
  };
};

export const fetchExtractors = (inputId: string): Promise<{ extractors: Array<ExtractorType> }> =>
  Extractors.list(inputId);

export const fetchExtractor = (inputId: string, extractorId: string): Promise<ExtractorType> =>
  Extractors.single(inputId, extractorId);

export const newExtractor = (type: string, field: string) => {
  if (ExtractorUtils.EXTRACTOR_TYPES.indexOf(type) === -1) {
    throw new Error(`Invalid extractor type provided: ${type}`);
  }

  return {
    type,
    source_field: field,
    converters: [],
    extractor_config: {},
    target_field: '',
  };
};

export const createExtractor = (inputId: string, extractor: ExtractorType): Promise<unknown> => {
  const promise = Extractors.create(inputId, getExtractorDTO(extractor));

  promise.then(
    () => UserNotification.success(i18n.t('use_extractors_notifications.created_success', { ns: 'extractors', title: extractor.title })),
    (error) => UserNotification.error(i18n.t('use_extractors_notifications.create_failed', { ns: 'extractors', error }), i18n.t('use_extractors_notifications.create_failed_title', { ns: 'extractors' })),
  );

  return promise;
};

export const updateExtractor = (inputId: string, extractor: ExtractorType): Promise<unknown> => {
  const promise = Extractors.update(inputId, extractor.id, getExtractorDTO(extractor));

  promise.then(
    () => UserNotification.success(i18n.t('use_extractors_notifications.updated_success', { ns: 'extractors', title: extractor.title })),
    (error) => UserNotification.error(i18n.t('use_extractors_notifications.update_failed', { ns: 'extractors', error }), i18n.t('use_extractors_notifications.update_failed_title', { ns: 'extractors' })),
  );

  return promise;
};

export const saveExtractor = (inputId: string, extractor: ExtractorType): Promise<unknown> => {
  if (extractor.id) {
    return updateExtractor(inputId, extractor);
  }

  return createExtractor(inputId, extractor);
};

export const deleteExtractor = (inputId: string, extractor: ExtractorType): Promise<unknown> => {
  const promise = Extractors.terminate(inputId, extractor.id);

  promise.then(
    () => UserNotification.success(i18n.t('use_extractors_notifications.deleted_success', { ns: 'extractors', title: extractor.title })),
    (error) =>
      UserNotification.error(i18n.t('use_extractors_notifications.delete_failed', { ns: 'extractors', error }), i18n.t('use_extractors_notifications.delete_failed_title', { ns: 'extractors', title: extractor.title })),
  );

  return promise;
};

export const orderExtractors = (inputId: string, orderedExtractors: Array<ExtractorType>): Promise<unknown> => {
  const orderedExtractorsMap = {};

  orderedExtractors.forEach((extractor, idx) => {
    orderedExtractorsMap[idx] = extractor.id;
  });

  const promise = Extractors.order(inputId, { order: orderedExtractorsMap });

  promise.then(
    () => UserNotification.success(i18n.t('use_extractors_notifications.positions_updated_success', { ns: 'extractors' })),
    (error) =>
      UserNotification.error(i18n.t('use_extractors_notifications.positions_failed', { ns: 'extractors', error }), i18n.t('use_extractors_notifications.positions_failed_title', { ns: 'extractors' })),
  );

  return promise;
};

export const importExtractors = async (
  inputId: string,
  extractors: Array<ExtractorType>,
): Promise<{ successful: number; failed: number }> => {
  let successfulImports = 0;
  let failedImports = 0;
  const promises = extractors.map((extractor) =>
    Extractors.create(inputId, getExtractorDTO(extractor))
      .then(() => {
        successfulImports += 1;
      })
      .catch(() => {
        failedImports += 1;
      }),
  );

  await Promise.allSettled(promises);

  if (failedImports === 0) {
    UserNotification.success(
      i18n.t('use_extractors_notifications.import_success', { ns: 'extractors', successful: successfulImports }),
      i18n.t('use_extractors_notifications.import_success_title', { ns: 'extractors' }),
    );
  } else {
    UserNotification.warning(
      i18n.t('use_extractors_notifications.import_warning', { ns: 'extractors', successful: successfulImports, failed: failedImports }),
      i18n.t('use_extractors_notifications.import_warning_title', { ns: 'extractors' }),
    );
  }

  return { successful: successfulImports, failed: failedImports };
};

export const useExtractors = (inputId: string) =>
  useQuery({
    queryKey: [...EXTRACTORS_QUERY_KEY, 'list', inputId],
    queryFn: () => fetchExtractors(inputId),
    select: (response) => response.extractors,
    enabled: !!inputId,
  });

export const useExtractor = (inputId: string, extractorId: string) =>
  useQuery({
    queryKey: [...EXTRACTORS_QUERY_KEY, 'detail', inputId, extractorId],
    queryFn: () => fetchExtractor(inputId, extractorId),
    enabled: !!(inputId && extractorId),
  });
