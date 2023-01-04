import { useEffect } from 'react';

import type View from 'views/logic/views/View';
import { ViewActions } from 'views/stores/ViewStore';

const useLoadView = (viewPromise: Promise<View>, queryId?: string) => {
  useEffect(() => {
    viewPromise.then((view) => ViewActions.loadNew(view, queryId));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [viewPromise]);
};

export default useLoadView;
