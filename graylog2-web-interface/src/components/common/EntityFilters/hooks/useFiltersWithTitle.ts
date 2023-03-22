import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useCallback } from 'react';

import fetch from 'logic/rest/FetchProvider';
import URLUtils from 'util/URLUtils';
import UserNotification from 'util/UserNotification';
import useUserDateTime from 'hooks/useUserDateTime';
import {
  extractRangeFromString,
  timeRangeTitle,
} from 'components/common/EntityFilters/FilterConfiguration/DateRangeForm';
import type { UrlQueryFilters, Filters } from 'components/common/EntityFilters/types';
import type { Attributes, Attribute } from 'stores/PaginationTypes';
import type { DateTime } from 'util/DateTime';

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
  Object.entries(filters).reduce((col, [attributeId, filterValues]) => {
    const relatedCollection = collectionsByAttributeId?.[attributeId];

    if (!relatedCollection) {
      return col;
    }

    return {
      ...col,
      [attributeId]: filterValues,
    };
  }, {})
);

const filtersWithoutTitlePayload = (filtersWithoutTitle: UrlQueryFilters, collectionsByAttributeId: CollectionsByAttributeId) => ({
  entities: Object.entries(filtersWithoutTitle).reduce((col, [attributeId, filterValues]) => {
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
) => {
  if (attribute?.type === 'DATE') {
    const [from, until] = extractRangeFromString(filterValue);

    const fromDate = from ? formatTime(from) : undefined;
    const untilDate = until ? formatTime(until) : undefined;

    return timeRangeTitle(fromDate, untilDate);
  }

  if (attribute?.filter_options) {
    const relatedOption = attribute.filter_options.find(({ value }) => value === filterValue);

    return relatedOption?.title ?? filterValue;
  }

  if (attribute?.related_collection) {
    const fetchedTitle = requestedFilterTitles?.find(({ id, type }) => (type === attribute.related_collection && id === filterValue))?.title;

    return fetchedTitle ?? 'Loading...';
  }

  return filterValue;
};

const allfiltersWithTitle = (
  urlQueryFilters: UrlQueryFilters,
  requestedFilterTitles: RequestedFilterTitles,
  attributesMetaData: Attributes,
  formatTime: (dateTime: DateTime) => string,
) => {
  return Object.entries(urlQueryFilters).reduce((col, [attributeId, filterValues]) => {
    const relatedAttribute = attributesMetaData?.find(({ id }) => id === attributeId);
    const filtersWithTitle = filterValues.map((value) => {
      const title = filterTitle(relatedAttribute, requestedFilterTitles, value, formatTime);

      return ({
        id: value,
        title,
        value,
      });
    });

    return {
      ...col,
      [attributeId]: filtersWithTitle,
    };
  }, {});
};

const filtersWithTitleToResponse = (filtersWithTitle: Filters, attributesMetaData: Attributes) => Object.entries(filtersWithTitle).reduce(
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
  fetch('POST', URLUtils.qualifyUrl('/system/catalog/entity_titles'), payload)
);

const useFiltersWithTitle = (urlQueryFilters: UrlQueryFilters, attributesMetaData: Attributes, enabled: boolean) => {
  const queryClient = useQueryClient();
  const { formatTime } = useUserDateTime();
  const collectionsByAttributeId = _collectionsByAttributeId(attributesMetaData);
  const urlQueryFiltersWithoutTitle = _urlQueryFiltersWithoutTitle(urlQueryFilters, collectionsByAttributeId);
  const payload = filtersWithoutTitlePayload(urlQueryFiltersWithoutTitle, collectionsByAttributeId);

  const { data, refetch, isInitialLoading } = useQuery(
    ['entity_titles', payload],
    () => fetchFilterTitles(payload),
    {
      onError: (errorThrown) => {
        UserNotification.error(`Loading filter titles failed with status: ${errorThrown}`,
          'Could not load streams');
      },
      keepPreviousData: true,
      enabled,
    },
  );

  const cachedResponse = queryClient.getQueryData(['entity_titles', payload]);
  const requestedFilterTitles = (cachedResponse ?? data)?.entities;
  const allFiltersWithTitle = allfiltersWithTitle(urlQueryFilters, requestedFilterTitles, attributesMetaData, formatTime);

  const onChange = useCallback((newFiltersWithTitle: Filters, newUrlQueryFilters: UrlQueryFilters) => {
    const newURLQueryFiltersWithoutTitle = _urlQueryFiltersWithoutTitle(newUrlQueryFilters, collectionsByAttributeId);
    const newPayload = filtersWithoutTitlePayload(newURLQueryFiltersWithoutTitle, collectionsByAttributeId);
    const newResponse = filtersWithTitleToResponse(newFiltersWithTitle, attributesMetaData);

    queryClient.setQueryData(['entity_titles', newPayload], { entities: newResponse });
  }, [attributesMetaData, collectionsByAttributeId, queryClient]);

  return ({
    data: allFiltersWithTitle,
    refetch,
    isInitialLoading,
    onChange,
  });
};

export default useFiltersWithTitle;
