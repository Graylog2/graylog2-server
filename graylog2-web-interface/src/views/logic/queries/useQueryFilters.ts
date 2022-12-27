import { useStore } from 'stores/connect';
import { QueriesStore } from 'views/stores/QueriesStore';
import type { StoreState } from 'stores/StoreTypes';

const queriesStoreMapper = (newQueries: StoreState<typeof QueriesStore>) => newQueries.map((q) => q.filter).toMap();

const useQueryFilters = () => useStore(QueriesStore, queriesStoreMapper);

export default useQueryFilters;
