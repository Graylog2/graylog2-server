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

import ApiRoutes from 'routing/ApiRoutes';
import fetch from 'logic/rest/FetchProvider';
import ExtractorUtils from 'util/ExtractorUtils';
import * as URLUtils from 'util/URLUtils';
import UserNotification from 'util/UserNotification';

const SOURCE_URL = '/system/inputs/';
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
  fetch('GET', URLUtils.qualifyUrl(URLUtils.concatURLPath(SOURCE_URL, inputId, 'extractors')));

export const fetchExtractor = (inputId: string, extractorId: string): Promise<ExtractorType> =>
  fetch('GET', URLUtils.qualifyUrl(URLUtils.concatURLPath(SOURCE_URL, inputId, 'extractors', extractorId)));

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
  const url = URLUtils.qualifyUrl(ApiRoutes.ExtractorsController.create(inputId).url);
  const promise = fetch('POST', url, getExtractorDTO(extractor));

  promise.then(
    () => UserNotification.success(`Extractor ${extractor.title} created successfully`),
    (error) => UserNotification.error(`Creating extractor failed: ${error}`, 'Could not create extractor'),
  );

  return promise;
};

export const updateExtractor = (inputId: string, extractor: ExtractorType): Promise<unknown> => {
  const url = URLUtils.qualifyUrl(ApiRoutes.ExtractorsController.update(inputId, extractor.id).url);
  const promise = fetch('PUT', url, getExtractorDTO(extractor));

  promise.then(
    () => UserNotification.success(`Extractor "${extractor.title}" updated successfully`),
    (error) => UserNotification.error(`Updating extractor failed: ${error}`, 'Could not update extractor'),
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
  const url = URLUtils.qualifyUrl(ApiRoutes.ExtractorsController.delete(inputId, extractor.id).url);
  const promise = fetch('DELETE', url);

  promise.then(
    () => UserNotification.success(`Extractor "${extractor.title}" deleted successfully`),
    (error) =>
      UserNotification.error(`Deleting extractor failed: ${error}`, `Could not delete extractor ${extractor.title}`),
  );

  return promise;
};

export const orderExtractors = (inputId: string, orderedExtractors: Array<ExtractorType>): Promise<unknown> => {
  const url = URLUtils.qualifyUrl(ApiRoutes.ExtractorsController.order(inputId).url);
  const orderedExtractorsMap = {};

  orderedExtractors.forEach((extractor, idx) => {
    orderedExtractorsMap[idx] = extractor.id;
  });

  const promise = fetch('POST', url, { order: orderedExtractorsMap });

  promise.then(
    () => UserNotification.success('Extractor positions updated successfully'),
    (error) =>
      UserNotification.error(`Changing extractor positions failed: ${error}`, 'Could not update extractor positions'),
  );

  return promise;
};

export const importExtractors = async (
  inputId: string,
  extractors: Array<ExtractorType>,
): Promise<{ successful: number; failed: number }> => {
  let successfulImports = 0;
  let failedImports = 0;
  const promises = extractors.map((extractor) => {
    const url = URLUtils.qualifyUrl(ApiRoutes.ExtractorsController.create(inputId).url);

    return fetch('POST', url, getExtractorDTO(extractor))
      .then(() => {
        successfulImports += 1;
      })
      .catch(() => {
        failedImports += 1;
      });
  });

  await Promise.allSettled(promises);

  if (failedImports === 0) {
    UserNotification.success(
      `Import results: ${successfulImports} extractor(s) imported.`,
      'Import operation successful',
    );
  } else {
    UserNotification.warning(
      `Import results: ${successfulImports} extractor(s) imported, ${failedImports} error(s).`,
      'Import operation completed',
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
