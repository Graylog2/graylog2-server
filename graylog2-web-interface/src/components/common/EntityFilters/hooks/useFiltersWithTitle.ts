import { useQuery, useQueryClient } from '@tanstack/react-query';

import fetch from 'logic/rest/FetchProvider';
import URLUtils from 'util/URLUtils';
import UserNotification from 'util/UserNotification';

const fetchFilterTitles = (filterParams: Array<{ id: string, type: string }>) => fetch('POST', URLUtils.qualifyUrl('/system/catalog/entity_titles'), { entities: filterParams });

const useFiltersWithTitle = (filters: { [attributeId: string]: Array<string> }, attributesMetaData: Array<{ id: string, related_collection?: string }> | undefined, enabled: boolean) => {
  const queryClient = useQueryClient();

  const filterParams = Object.entries(filters).reduce((col, [attributeId, values]) => {
    const relatedAttribute = attributesMetaData?.find((attribute) => attribute.id === attributeId);

    if (!relatedAttribute) {
      return col;
    }

    return [
      ...col,
      ...values.map((value) => ({
        id: value,
        type: relatedAttribute.related_collection,
      })),
    ];
  }, []);

  const cachedData = queryClient.getQueryData(['entity_titles', filters]);
  const { data, refetch, isInitialLoading } = useQuery(
    ['entity_titles', filters],
    () => fetchFilterTitles(filterParams),
    {
      onError: (errorThrown) => {
        UserNotification.error(`Loading filter titles failed with status: ${errorThrown}`,
          'Could not load streams');
      },
      keepPreviousData: true,
      enabled,
    },
  );

  return ({
    data: (cachedData ?? data)?.entities?.reduce((col, { id, title, type }) => {
      const relatedAttribute = attributesMetaData.find((attribute) => attribute.related_collection === type);

      if (!relatedAttribute) {
        return col;
      }

      return {
        ...col,
        [relatedAttribute.id]: [...col[relatedAttribute.id] ?? [], { id, title }],
      };
    }, {}),
    refetch,
    isInitialLoading,
  });
};

export default useFiltersWithTitle;
