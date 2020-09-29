// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';

import useViewLoader from 'views/logic/views/UseViewLoader';
import Spinner from 'components/common/Spinner';
import View from 'views/logic/views/View';
import ViewLoaderContext from 'views/logic/ViewLoaderContext';
import type { ViewLoaderFn } from 'views/logic/views/ViewLoader';
import ViewLoader from 'views/logic/views/ViewLoader';
import NewViewLoaderContext from 'views/logic/NewViewLoaderContext';
import { ExtendedSearchPage } from 'views/pages';
import withParams from 'routing/withParams';
import { loadNewView, loadView } from 'views/logic/views/Actions';

type Props = {
  location: {
    state?: {
      view: ?View,
      widgetId: ?string,
    },
    query: { [string]: any },
  },
  params: {
    viewId: string,
  },
  route: any,
  viewLoader: ViewLoaderFn,
};

const ShowViewPage = ({ params: { viewId }, route, location: { query }, viewLoader }: Props) => {
  const [loaded, HookComponent] = useViewLoader(viewId, query, viewLoader);

  if (HookComponent) {
    return HookComponent;
  }

  if (!loaded) {
    return <Spinner />;
  }

  return (
    <NewViewLoaderContext.Provider value={loadNewView}>
      <ViewLoaderContext.Provider value={loadView}>
        <ExtendedSearchPage route={route} />
      </ViewLoaderContext.Provider>
    </NewViewLoaderContext.Provider>
  );
};

ShowViewPage.propTypes = {
  location: PropTypes.shape({
    query: PropTypes.object,
    state: PropTypes.shape({
      view: PropTypes.object,
      widgetId: PropTypes.string,
    }),
  }).isRequired,
  params: PropTypes.shape({
    viewId: PropTypes.string.isRequired,
  }).isRequired,
  route: PropTypes.object.isRequired,
  viewLoader: PropTypes.func,
};

ShowViewPage.defaultProps = {
  viewLoader: ViewLoader,
};

export default withParams(ShowViewPage);
