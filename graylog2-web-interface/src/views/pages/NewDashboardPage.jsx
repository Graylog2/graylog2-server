// @flow strict
import React, { useEffect, useState } from 'react';
import PropTypes from 'prop-types';
import Spinner from 'components/common/Spinner';

import { ViewActions } from 'views/stores/ViewStore';
import View from 'views/logic/views/View';
import ViewTypeContext from 'views/components/contexts/ViewTypeContext';
import ExtendedSearchPage from './ExtendedSearchPage';

type Props = {
  route: {},
};
const NewDashboardPage = ({ route }: Props) => {
  const [loaded, setLoaded] = useState(false);
  useEffect(() => {
    ViewActions.create().then(() => setLoaded(true));
  }, []);

  return loaded
    ? (
      <ViewTypeContext.Provider value={View.Type.Dashboard}>
        <ExtendedSearchPage route={route} />
      </ViewTypeContext.Provider>
    )
    : <Spinner />;
};

NewDashboardPage.propTypes = {
  route: PropTypes.object.isRequired,
};

export default NewDashboardPage;
