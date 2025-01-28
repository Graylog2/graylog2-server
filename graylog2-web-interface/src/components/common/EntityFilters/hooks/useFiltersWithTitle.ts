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
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useCallback } from 'react';
import { OrderedMap } from 'immutable';

import fetch from 'logic/rest/FetchProvider';
import URLUtils from 'util/URLUtils';
import useUserDateTime from 'hooks/useUserDateTime';
import type { UrlQueryFilters, Filters, Filter } from 'components/common/EntityFilters/types';
import type { Attributes, Attribute } from 'stores/PaginationTypes';
import type { DateTime } from 'util/DateTime';
import {
  isDateAttribute,
  isAttributeWithFilterOptions,
  isAttributeWithRelatedCollection,
} from 'components/common/EntityFilters/helpers/AttributeIdentification';
import { timeRangeTitle, extractRangeFromString } from 'components/common/EntityFilters/helpers/timeRange';
import { defaultOnError } from 'util/conditional/onError';

type CollectionsByAttributeId = {
  [attributeId: string]: string | undefined
}

type RequestedFilterTitles = Array<{
  id: string,
  title; string,
  type: string,
}>

const _collectionsByAttributeId = (attributesMetaData: Attributes) => attributesMetaData?.reduce(
  (col, { id, related_collection }) => {
    if (!related_collection) {
      return col;
    }

    return {
      ...col,
      [id]: related_collection,
    };
  }, {});

const _urlQueryFiltersWithoutTitle = (filters: UrlQueryFilters, collectionsByAttributeId: CollectionsByAttributeId) => (
  filters.entrySeq().reduce((col, [attributeId, filterValues]) => {
    const relatedCollection = collectionsByAttributeId?.[attributeId];

    if (!relatedCollection) {
      return col;
    }

    return col.set(attributeId, filterValues);
  }, OrderedMap<string, Array<string>>())
);

const filtersWithoutTitlePayload = (filtersWithoutTitle: UrlQueryFilters, collectionsByAttributeId: CollectionsByAttributeId) => ({
  entities: filtersWithoutTitle.entrySeq().reduce((col, [attributeId, filterValues]) => {
    const relatedCollection = collectionsByAttributeId[attributeId];

    return [
      ...col,
      ...filterValues.map((value) => ({
        id: value,
        type: relatedCollection,
      })),
    ];
  }, []),
});

const filterTitle = (
  attribute: Attribute,
  requestedFilterTitles,
  filterValue: string,
  formatTime: (dateTime: DateTime) => string,
  notPermittedEntities: Array<string> | undefined,
  isErrorFetchingTitles: boolean,
) => {
  if (isDateAttribute(attribute)) {
    const [from, until] = extractRangeFromString(filterValue);

    const fromDate = from ? formatTime(from) : undefined;
    const untilDate = until ? formatTime(until) : undefined;

    return timeRangeTitle(fromDate, untilDate);
  }

  if (isAttributeWithFilterOptions(attribute)) {
    const relatedOption = attribute.filter_options.find(({ value }) => value === filterValue);

    return relatedOption?.title ?? filterValue;
  }

  if (isAttributeWithRelatedCollection(attribute)) {
    if (isErrorFetchingTitles || notPermittedEntities?.includes(filterValue)) {
      return filterValue;
    }

    const fetchedTitle = requestedFilterTitles?.find(({ id, type }) => (type === attribute.related_collection && id === filterValue))?.title;

    return fetchedTitle ?? 'Loading...';
  }

  return filterValue;
};

const _allFiltersWithTitle = (
  urlQueryFilters: UrlQueryFilters,
  requestedFilterTitles: RequestedFilterTitles,
  attributesMetaData: Attributes,
  formatTime: (dateTime: DateTime) => string,
  notPermittedEntities: Array<string> | undefined,
  isErrorFetchingTitles: boolean,
): Filters => (
  urlQueryFilters.entrySeq().reduce((col, [attributeId, filterValues]) => {
    const relatedAttribute = attributesMetaData?.find(({ id }) => id === attributeId);
    const filtersWithTitle: Array<Filter> = filterValues.map((value) => {
      const title = filterTitle(relatedAttribute, requestedFilterTitles, value, formatTime, notPermittedEntities, isErrorFetchingTitles);

      return ({
        title,
        value,
      });
    });

    return col.set(attributeId, filtersWithTitle);
  }, OrderedMap<string, Array<Filter>>())
);

const filtersWithTitleToResponse = (filtersWithTitle: Filters, attributesMetaData: Attributes) => filtersWithTitle.entrySeq().reduce(
  (col, [_attributeId, filters]) => {
    const relatedCollection = attributesMetaData?.find(({ id }) => id === _attributeId)?.related_collection;

    if (!relatedCollection) {
      return col;
    }

    return [
      ...col,
      ...filters.map(({ value, title }) => ({ id: value, type: relatedCollection, title })),
    ];
  }, []);

const fetchFilterTitles = (payload: { entities: Array<{ id: string, type: string }> }) => (
  fetch('POST', URLUtils.qualifyUrl('/system/catalog/entities/titles'), payload)
);

const useFiltersWithTitle = (
  urlQueryFilters: UrlQueryFilters,
  attributesMetaData: Attributes,
  enabled: boolean = true,
): {
  data: Filters
  onChange: (newFiltersWithTitle: Filters, newUrlQueryFilters: UrlQueryFilters) => void,
  isInitialLoading: boolean,
} => {
  const queryClient = useQueryClient();
  const { formatTime } = useUserDateTime();
  const collectionsByAttributeId = _collectionsByAttributeId(attributesMetaData);
  const urlQueryFiltersWithoutTitle = _urlQueryFiltersWithoutTitle(urlQueryFilters, collectionsByAttributeId);
  const payload = filtersWithoutTitlePayload(urlQueryFiltersWithoutTitle, collectionsByAttributeId);
  const { data, isInitialLoading, isError } = useQuery(
    ['entity_titles', payload],
    () => defaultOnError(fetchFilterTitles(payload), 'Loading filter titles failed with status', 'Could not load streams'),
    {
      keepPreviousData: true,
      enabled: enabled && !!payload.entities.length,
    },
  );

  const cachedResponse = queryClient.getQueryData(['entity_titles', payload]);
  const requestedFilterTitles = (cachedResponse ?? data)?.entities;
  const allFiltersWithTitle = _allFiltersWithTitle(urlQueryFilters, requestedFilterTitles, attributesMetaData, formatTime, data?.not_permitted_to_view, isError);

  const onChange = useCallback((newFiltersWithTitle: Filters, newUrlQueryFilters: UrlQueryFilters) => {
    const newURLQueryFiltersWithoutTitle = _urlQueryFiltersWithoutTitle(newUrlQueryFilters, collectionsByAttributeId);
    const newPayload = filtersWithoutTitlePayload(newURLQueryFiltersWithoutTitle, collectionsByAttributeId);
    const newResponse = filtersWithTitleToResponse(newFiltersWithTitle, attributesMetaData);

    queryClient.setQueryData(['entity_titles', newPayload], { entities: newResponse });
  }, [attributesMetaData, collectionsByAttributeId, queryClient]);

  return ({
    data: allFiltersWithTitle,
    isInitialLoading,
    onChange,
  });
};

export default useFiltersWithTitle;
