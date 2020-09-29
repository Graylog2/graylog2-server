// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import { withRouter } from 'react-router';

import { Spinner } from 'components/common';
import NewViewLoaderContext from 'views/logic/NewViewLoaderContext';
import ViewLoaderContext from 'views/logic/ViewLoaderContext';
import { ExtendedSearchPage } from 'views/pages';
import { loadNewView, loadView } from 'views/logic/views/Actions';
import IfUserHasAccessToAnyStream from 'views/components/IfUserHasAccessToAnyStream';
import useLoadView from 'views/logic/views/UseLoadView';
import useCreateSavedSearch from 'views/logic/views/UseCreateSavedSearch';

type URLQuery = { [string]: any };

type Props = {
  route: {},
  location: {
    query: URLQuery,
    pathname: string,
  },
};

const NewSearchPage = ({ location: { query }, location, route }: Props) => {
  const view = useCreateSavedSearch();
  const [loaded, HookComponent] = useLoadView(view, query);

  if (HookComponent) {
    return <HookComponent />;
  }

  if (loaded) {
    return (
      <ViewLoaderContext.Provider value={loadView}>
        <NewViewLoaderContext.Provider value={loadNewView}>
          <IfUserHasAccessToAnyStream>
            <ExtendedSearchPage route={route} location={location} />
          </IfUserHasAccessToAnyStream>
        </NewViewLoaderContext.Provider>
      </ViewLoaderContext.Provider>
    );
  }

  return <Spinner />;
};

NewSearchPage.propTypes = {
  route: PropTypes.object.isRequired,
  location: PropTypes.shape({
    query: PropTypes.object,
    pathname: PropTypes.string,
  }).isRequired,
};

export default withRouter(NewSearchPage);
