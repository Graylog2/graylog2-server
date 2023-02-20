import { useStore } from 'stores/connect';
import { RefreshStore } from 'views/stores/RefreshStore';

const useRefreshConfig = () => useStore(RefreshStore);
export default useRefreshConfig;
