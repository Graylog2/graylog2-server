// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';

import useViewLoader from 'views/logic/views/UseViewLoader';
import Spinner from 'components/common/Spinner';
import View from 'views/logic/views/View';
import type { ViewLoaderFn } from 'views/logic/views/ViewLoader';
import ViewLoader from 'views/logic/views/ViewLoader';
import withLocation from 'routing/withLocation';
import withParams from 'routing/withParams';
import type { Location } from 'routing/withLocation';

import SearchPage from './SearchPage';

type Props = {
  location: Location & {
    state?: {
      view: ?View,
      widgetId: ?string,
    },
  },
  params: {
    viewId: ?string,
  },
  viewLoader: ViewLoaderFn,
};

const ShowViewPage = ({ params: { viewId }, location: { query }, viewLoader }: Props) => {
  if (!viewId) {
    throw new Error('No view id specified!');
  }

  const [loaded, HookComponent] = useViewLoader(viewId, query, viewLoader);

  if (HookComponent) {
    return HookComponent;
  }

  if (!loaded) {
    return <Spinner />;
  }

  return <SearchPage />;
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
  viewLoader: PropTypes.func,
};

ShowViewPage.defaultProps = {
  viewLoader: ViewLoader,
};

export default withParams(withLocation(ShowViewPage));
