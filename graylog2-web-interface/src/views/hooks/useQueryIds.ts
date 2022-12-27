import { useStore } from 'stores/connect';
import type { ViewStoreState } from 'views/stores/ViewStore';
import { ViewStore } from 'views/stores/ViewStore';

const queryIdsMapper = (view: ViewStoreState) => view?.view?.search?.queries?.map((q) => q.id).toOrderedSet();
const useQueryIds = () => useStore(ViewStore, queryIdsMapper);

export default useQueryIds;
