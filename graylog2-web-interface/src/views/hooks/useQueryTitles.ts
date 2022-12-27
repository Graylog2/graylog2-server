import * as Immutable from 'immutable';

import { useStore } from 'stores/connect';
import type { ViewStoreState } from 'views/stores/ViewStore';
import { ViewStore } from 'views/stores/ViewStore';

const queryTitlesMapper = ({ view }: ViewStoreState) => {
  const viewState = view?.state ?? Immutable.Map();

  return viewState.map((state) => state.titles.getIn(['tab', 'title']) as string).filter((v) => v !== undefined).toMap();
};

const useQueryTitles = () => useStore(ViewStore, queryTitlesMapper);

export default useQueryTitles;
