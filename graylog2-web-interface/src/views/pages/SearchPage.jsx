// @flow strict
import * as React from 'react';

import ViewLoaderContext from 'views/logic/ViewLoaderContext';
import NewViewLoaderContext from 'views/logic/NewViewLoaderContext';
import Search from 'views/components/Search';
import { loadNewView as defaultLoadNewView, loadView as defaultLoadView } from 'views/logic/views/Actions';
import IfUserHasAccessToAnyStream from 'views/components/IfUserHasAccessToAnyStream';

type Props = {
  loadNewView?: () => mixed,
  loadView?: (string) => mixed,
  route: any,
};

const SearchPage = ({ loadNewView = defaultLoadNewView, loadView = defaultLoadView, route }: Props) => {
  return (
    <NewViewLoaderContext.Provider value={loadNewView}>
      <ViewLoaderContext.Provider value={loadView}>
        <IfUserHasAccessToAnyStream>
          <Search route={route} />
        </IfUserHasAccessToAnyStream>
      </ViewLoaderContext.Provider>
    </NewViewLoaderContext.Provider>
  );
};

SearchPage.defaultProps = {
  loadNewView: defaultLoadNewView,
  loadView: defaultLoadView,
};

export default SearchPage;
