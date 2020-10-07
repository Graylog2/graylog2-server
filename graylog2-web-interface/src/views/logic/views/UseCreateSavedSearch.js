// @flow strict
import { useMemo } from 'react';

import { ViewActions } from 'views/stores/ViewStore';
import View from 'views/logic/views/View';

const useCreateSavedSearch = (streamId: ?string) => useMemo(() => ViewActions.create(View.Type.Search, streamId)
  .then(({ view: v }) => v), [streamId]);

export default useCreateSavedSearch;
