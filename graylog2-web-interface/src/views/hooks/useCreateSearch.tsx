import { useMemo } from 'react';

import createSearch from 'views/logic/slices/createSearch';
import type View from 'views/logic/views/View';

const useCreateSearch = (viewPromise: Promise<View>) => {
  return useMemo(() => viewPromise.then(async (_view) => {
    await createSearch(_view.search);

    return _view;
  }), [viewPromise]);
};

export default useCreateSearch;
