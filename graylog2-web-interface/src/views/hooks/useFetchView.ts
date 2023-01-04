import { useMemo } from 'react';

import ErrorsActions from 'actions/errors/ErrorsActions';
import { ViewManagementActions } from 'views/stores/ViewManagementStore';
import ViewDeserializer from 'views/logic/views/ViewDeserializer';
import { createFromFetchError } from 'logic/errors/ReportedErrors';

const useFetchView = (viewId: string) => {
  const viewJsonPromise = useMemo(() => ViewManagementActions.get(viewId), [viewId]);

  return useMemo(() => viewJsonPromise.then((viewJson) => ViewDeserializer(viewJson), (error) => {
    if (error.status === 404) {
      ErrorsActions.report(createFromFetchError(error));
    }

    throw error;
  }), [viewJsonPromise]);
};

export default useFetchView;
