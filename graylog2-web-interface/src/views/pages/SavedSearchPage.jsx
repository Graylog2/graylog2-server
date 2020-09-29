// @flow strict
import * as React from 'react';

import ViewLoaderContext from 'views/logic/ViewLoaderContext';
import NewViewLoaderContext from 'views/logic/NewViewLoaderContext';
import { ExtendedSearchPage } from 'views/pages';
import { loadNewView, loadView } from 'views/logic/views/Actions';
import IfUserHasAccessToAnyStream from 'views/components/IfUserHasAccessToAnyStream';

type Props = {
  route: any,
};

const SavedSearchPage = ({ route }: Props) => (
  <NewViewLoaderContext.Provider value={loadNewView}>
    <ViewLoaderContext.Provider value={loadView}>
      <IfUserHasAccessToAnyStream>
        <ExtendedSearchPage route={route} />
      </IfUserHasAccessToAnyStream>
    </ViewLoaderContext.Provider>
  </NewViewLoaderContext.Provider>
);

export default SavedSearchPage;
