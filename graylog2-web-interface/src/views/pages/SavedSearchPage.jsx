// @flow strict
import * as React from 'react';

import ViewLoaderContext from 'views/logic/ViewLoaderContext';
import NewViewLoaderContext from 'views/logic/NewViewLoaderContext';
import { ExtendedSearchPage } from 'views/pages';
import { loadNewView as defaultLoadNewView, loadView as defaultLoadView } from 'views/logic/views/Actions';
import IfUserHasAccessToAnyStream from 'views/components/IfUserHasAccessToAnyStream';

type Props = {
  loadNewView?: () => mixed,
  loadView?: (string) => mixed,
  route: any,
};

const SavedSearchPage = ({ loadNewView = defaultLoadNewView, loadView = defaultLoadView, route }: Props) => (
  <NewViewLoaderContext.Provider value={loadNewView}>
    <ViewLoaderContext.Provider value={loadView}>
      <IfUserHasAccessToAnyStream>
        <ExtendedSearchPage route={route} />
      </IfUserHasAccessToAnyStream>
    </ViewLoaderContext.Provider>
  </NewViewLoaderContext.Provider>
);

SavedSearchPage.defaultProps = {
  loadNewView: defaultLoadNewView,
  loadView: defaultLoadView,
};

export default SavedSearchPage;
