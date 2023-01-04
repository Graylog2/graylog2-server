import { useEffect } from 'react';

import type View from 'views/logic/views/View';
import { ViewActions } from 'views/stores/ViewStore';

const useLoadView = (viewPromise: Promise<View>) => {
  useEffect(() => {
    viewPromise.then(ViewActions.loadNew);
  }, [viewPromise]);
};

export default useLoadView;
