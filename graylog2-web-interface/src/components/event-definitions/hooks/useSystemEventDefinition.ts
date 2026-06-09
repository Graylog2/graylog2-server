import type { SearchParams } from 'stores/PaginationTypes';
import useEventDefinitions from 'components/event-definitions/hooks/useEventDefinitions';
import { isSystemEventDefinition } from 'components/event-definitions/event-definitions-types';

const eventDefinitionsSearchParams: SearchParams = {
  page: 1,
  pageSize: 0,
  query: '',
  sort: {
    attributeId: 'title',
    direction: 'asc',
  },
};

const useSystemEventDefinition = () => {
  const { data: eventDefinitions, isInitialLoading } = useEventDefinitions(eventDefinitionsSearchParams);

  return {
    isLoading: isInitialLoading,
    systemEventDefinition: eventDefinitions?.list.find(isSystemEventDefinition),
  };
};

export default useSystemEventDefinition;
