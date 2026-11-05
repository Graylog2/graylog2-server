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
import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import PaginationURL from 'util/PaginationURL';
import FiltersForQueryParams from 'components/common/EntityFilters/FiltersForQueryParams';
import { fetchTextFile } from 'util/FileDownloadUtils';
import type { SearchParams } from 'stores/PaginationTypes';

import type { CreateTranslationFeedback, Status, TranslationFeedback } from './types';

const BASE_URL = '/system/translation_feedback';

export const keyFn = (searchParams: SearchParams) => ['translationFeedback', searchParams];

export const fetchTranslationFeedback = (searchParams: SearchParams) => {
  const url = PaginationURL(
    qualifyUrl(`${BASE_URL}/paginated`),
    searchParams.page,
    searchParams.pageSize,
    searchParams.query,
    {
      filters: FiltersForQueryParams(searchParams.filters),
      sort: searchParams.sort.attributeId,
      order: searchParams.sort.direction,
    },
  );

  return fetch('GET', url).then(({ elements, total, attributes }) => ({
    list: elements as Array<TranslationFeedback>,
    pagination: { total },
    attributes,
  }));
};

export const createTranslationFeedback = (feedback: CreateTranslationFeedback): Promise<TranslationFeedback> =>
  fetch('POST', qualifyUrl(BASE_URL), feedback);

export const updateTranslationFeedbackStatus = (id: string, status: Status): Promise<TranslationFeedback> =>
  fetch('PUT', qualifyUrl(`${BASE_URL}/${encodeURIComponent(id)}/status`), { status });

export const deleteTranslationFeedback = (id: string) =>
  fetch('DELETE', qualifyUrl(`${BASE_URL}/${encodeURIComponent(id)}`));

export type BulkResult = { changed: number; not_found: number };

export const bulkUpdateTranslationFeedbackStatus = (ids: Array<string>, status: Status): Promise<BulkResult> =>
  fetch('POST', qualifyUrl(`${BASE_URL}/bulk_status`), { ids, status });

export const bulkDeleteTranslationFeedback = (ids: Array<string>): Promise<BulkResult> =>
  fetch('POST', qualifyUrl(`${BASE_URL}/bulk_delete`), { ids });

export type Resolutions = { resolutions: Array<{ id: string; status: Status; note?: string }> };

export const applyTranslationFeedbackResolutions = (resolutions: Resolutions): Promise<BulkResult> =>
  fetch('POST', qualifyUrl(`${BASE_URL}/resolutions`), resolutions);

const EXPORT_MIME_TYPES = { json: 'application/json', csv: 'text/csv' };

export const exportTranslationFeedback = (
  format: 'json' | 'csv',
  searchParams: Pick<SearchParams, 'query' | 'filters'>,
) => {
  const params = [
    ...(searchParams.query ? [['query', searchParams.query]] : []),
    ...(FiltersForQueryParams(searchParams.filters) ?? []).map((filter) => ['filters', filter]),
  ];
  const query = params.map(([name, value]) => `${name}=${encodeURIComponent(value)}`).join('&');
  const url = qualifyUrl(`${BASE_URL}/export/${format}${query ? `?${query}` : ''}`);
  const date = new Date().toISOString().slice(0, 10);

  return fetchTextFile('GET', url, undefined, EXPORT_MIME_TYPES[format], `translation-feedback-${date}.${format}`);
};
