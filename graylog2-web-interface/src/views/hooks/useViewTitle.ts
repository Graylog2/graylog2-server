import { useStore } from 'stores/connect';
import type { ViewStoreState } from 'views/stores/ViewStore';
import { ViewStore } from 'views/stores/ViewStore';
import viewTitle from 'views/logic/views/ViewTitle';

const viewTitleMapper = (storeState: ViewStoreState) => viewTitle(storeState?.view?.title, storeState?.view?.type);
const useViewTitle = () => useStore(ViewStore, viewTitleMapper);

export default useViewTitle;
