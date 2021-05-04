import { useStore } from 'stores/connect';
import { CurrentQueryStore } from 'views/stores/CurrentQueryStore';
import Query from 'views/logic/queries/Query';

const useCurrentQuery = (): Query => useStore(CurrentQueryStore);
export default useCurrentQuery;
