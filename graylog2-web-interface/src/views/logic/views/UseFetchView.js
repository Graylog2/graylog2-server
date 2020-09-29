// @flow strict
import { useEffect, useState } from 'react';

import ErrorsActions from 'actions/errors/ErrorsActions';
import { createFromFetchError } from 'logic/errors/ReportedErrors';
import { ViewManagementActions } from 'views/stores/ViewManagementStore';

import ViewDeserializer from './ViewDeserializer';
import View from './View';

const useFetchView = (viewId: string): ?View => {
  const [view, setView] = useState();

  useEffect(() => {
    ViewManagementActions.get(viewId)
      .then(ViewDeserializer, (error) => {
        if (error.status === 404) {
          ErrorsActions.report(createFromFetchError(error));
        } else {
          throw error;
        }

        return View.create();
      }).then((v) => setView(v));
  }, [viewId]);

  return view;
};

export default useFetchView;
