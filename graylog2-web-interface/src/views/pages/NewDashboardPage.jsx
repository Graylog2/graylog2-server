// @flow strict
import React, { useEffect, useState } from 'react';
import PropTypes from 'prop-types';
import Spinner from 'components/common/Spinner';

import { ViewActions } from 'views/stores/ViewStore';
import View from 'views/logic/views/View';
import ExtendedSearchPage from './ExtendedSearchPage';

type Props = {
  route: {},
};
const NewDashboardPage = ({ route }: Props) => {
  const [loaded, setLoaded] = useState(false);
  useEffect(() => {
    ViewActions.create(View.Type.Dashboard).then(() => setLoaded(true));
  }, []);

  return loaded
    ? <ExtendedSearchPage route={route} />
    : <Spinner />;
};

NewDashboardPage.propTypes = {
  route: PropTypes.object.isRequired,
};

export default NewDashboardPage;
