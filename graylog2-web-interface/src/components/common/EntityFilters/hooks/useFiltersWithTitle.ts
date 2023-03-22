import { useQuery, useQueryClient } from '@tanstack/react-query';

import fetch from 'logic/rest/FetchProvider';
import URLUtils from 'util/URLUtils';
import UserNotification from 'util/UserNotification';
import useUserDateTime from 'hooks/useUserDateTime';
import {
  extractRangeFromString,
  timeRangeTitle,
} from 'components/common/EntityFilters/FilterConfiguration/DateRangeForm';
import type { UrlQueryFilters } from 'components/common/EntityFilters/types';
import type { Attributes } from 'stores/PaginationTypes';

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

const categorizeFilters = (filters: { [attributeId: string]: Array<string> }, collectionsByAttributeId: { [attributeId: string]: string | undefined }) => (
  Object.entries(filters).reduce((col, [attributeId, filterValues]) => {
    const relatedCollection = collectionsByAttributeId?.[attributeId];

    if (!relatedCollection) {
      return {
        ...col,
        filtersWithTitle: {
          ...col.filtersWithTitle,
          [attributeId]: filterValues,
        },
      };
    }

    return {
      ...col,
      filtersWithoutTitle: {
        ...col.filtersWithoutTitle,
        [attributeId]: filterValues,
      },
    };
  }, { filtersWithTitle: {}, filtersWithoutTitle: {} })
);

const _payload = (filtersWithoutTitle, collectionsByAttributeId) => ({
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

const filterTitle = (attribute, fetchedFilterTitles, filterValue, formatTime) => {
  if (attribute?.type === 'DATE') {
    const [from, until] = extractRangeFromString(filterValue);

    return timeRangeTitle(formatTime(from), formatTime(until));
  }

  if (attribute?.filter_options) {
    const relatedOption = attribute.filter_options.find(({ value }) => value === filterValue);

    return relatedOption?.title ?? filterValue;
  }

  if (attribute?.related_collection) {
    const fetchedTitle = fetchedFilterTitles?.find(({ id, type }) => (type === attribute.related_collection && id === filterValue))?.title;

    return fetchedTitle ?? 'Loading...';
  }

  return filterValue;
};

const _allFiltersWithTitle = (urlQueryFilters, fetchedFilterTitles, attributesMetaData, formatTime) => {
  return Object.entries(urlQueryFilters).reduce((col, [attributeId, filterValues]) => {
    const relatedAttribute = attributesMetaData?.find(({ id }) => id === attributeId);
    const filtersWithTitle = filterValues.map((value) => {
      const title = filterTitle(relatedAttribute, fetchedFilterTitles, value, formatTime);

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

const fetchFilterTitles = (payload: { entities: Array<{ id: string, type: string }> }) => fetch('POST', URLUtils.qualifyUrl('/system/catalog/entity_titles'), payload);

const useFiltersWithTitle = (urlQueryFilters: UrlQueryFilters, attributesMetaData: Attributes, enabled: boolean) => {
  const queryClient = useQueryClient();
  const { formatTime } = useUserDateTime();
  const collectionsByAttributeId = _collectionsByAttributeId(attributesMetaData);
  const categorizedFilters = categorizeFilters(urlQueryFilters, collectionsByAttributeId);
  const payload = _payload(categorizedFilters.filtersWithoutTitle, collectionsByAttributeId);

  const { data, refetch, isInitialLoading } = useQuery(
    ['entity_titles', urlQueryFilters],
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

  const cachedResponse = queryClient.getQueryData(['entity_titles', urlQueryFilters]);
  const fetchedFilterTitles = (cachedResponse ?? data)?.entities;
  const allFiltersWithTitle = _allFiltersWithTitle(urlQueryFilters, fetchedFilterTitles, attributesMetaData, formatTime);

  return ({
    data: allFiltersWithTitle,
    refetch,
    isInitialLoading,
  });
};

export default useFiltersWithTitle;
