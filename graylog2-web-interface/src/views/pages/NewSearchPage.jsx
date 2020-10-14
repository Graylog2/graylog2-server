// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';

import withLocation from 'routing/withLocation';
import type { Location } from 'routing/withLocation';
import { Spinner } from 'components/common';
import useLoadView from 'views/logic/views/UseLoadView';
import useCreateSavedSearch from 'views/logic/views/UseCreateSavedSearch';

import SearchPage from './SearchPage';

type Props = {
  location: Location,
};

const NewSearchPage = ({ location: { query } }: Props) => {
  const view = useCreateSavedSearch();
  const [loaded, HookComponent] = useLoadView(view, query);

  if (HookComponent) {
    return HookComponent;
  }

  if (!loaded) {
    return <Spinner />;
  }

  return <SearchPage />;
};

NewSearchPage.propTypes = {
  location: PropTypes.shape({
    query: PropTypes.object,
    pathname: PropTypes.string,
  }).isRequired,
};

export default withLocation(NewSearchPage);
