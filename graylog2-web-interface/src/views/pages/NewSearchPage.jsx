// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';

import withLocation from 'routing/withLocation';
import { Spinner } from 'components/common';
import useLoadView from 'views/logic/views/UseLoadView';
import useCreateSavedSearch from 'views/logic/views/UseCreateSavedSearch';

import SearchPage from './SearchPage';

type URLQuery = { [string]: any };

type Props = {
  route: {},
  location: {
    query: URLQuery,
    pathname: string,
  },
};

const NewSearchPage = ({ location: { query }, route }: Props) => {
  const view = useCreateSavedSearch();
  const [loaded, HookComponent] = useLoadView(view, query);

  if (HookComponent) {
    return HookComponent;
  }

  if (!loaded) {
    return <Spinner />;
  }

  return <SearchPage route={route} />;
};

NewSearchPage.propTypes = {
  route: PropTypes.object.isRequired,
  location: PropTypes.shape({
    query: PropTypes.object,
    pathname: PropTypes.string,
  }).isRequired,
};

export default withLocation(NewSearchPage);
