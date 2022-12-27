import { useStore } from 'stores/connect';
import type { ViewStoreState } from 'views/stores/ViewStore';
import { ViewStore } from 'views/stores/ViewStore';

const mapViewMetadata = ({ activeQuery, view }: ViewStoreState) => {
  if (view) {
    const { id, title, description, summary } = view;

    return { id, title, description, summary, activeQuery };
  }

  return {};
};

const useViewMetadata = () => useStore(ViewStore, mapViewMetadata);

export default useViewMetadata;
