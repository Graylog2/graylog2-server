// @flow strict
import React, { useEffect, useState } from 'react';

import { ViewActions } from 'views/stores/ViewStore';
import { QueryFiltersActions } from 'views/stores/QueryFiltersStore';
import View from 'views/logic/views/View';
import Spinner from 'components/common/Spinner';
import { ExtendedSearchPage } from 'views/pages';

type Props = {
  params: {
    streamId: string,
  },
  route: {}
};

export default ({ params: { streamId }, route }: Props) => {
  const [loaded, setLoaded] = useState(false);
  useEffect(() => {
    ViewActions.create(View.Type.Search)
      .then(({ activeQuery }) => QueryFiltersActions.streams(activeQuery, [streamId]))
      .then(() => setLoaded(true));
  }, []);

  return loaded ? <ExtendedSearchPage route={route} /> : <Spinner />;
};
