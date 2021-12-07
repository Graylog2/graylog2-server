import { useStore } from 'stores/connect'; import { SearchExecutionStateStore } from 'views/stores/SearchExecutionStateStore'; import { SearchStore } from 'views/stores/SearchStore';

const useParameters = () => {
  const { parameterBindings } = useStore(SearchExecutionStateStore);
  const { search: { parameters } } = useStore(SearchStore);

  return { parameterBindings, parameters };
};

export default useParameters;
