// @flow strict
import * as React from 'react';

import NewViewLoaderContext from 'views/logic/NewViewLoaderContext';
import ViewLoaderContext from 'views/logic/ViewLoaderContext';
import Spinner from 'components/common/Spinner';
import { ExtendedSearchPage } from 'views/pages';
import withParams from 'routing/withParams';
import useLoadView from 'views/logic/views/UseLoadView';
import { loadNewView, loadView } from 'views/logic/views/Actions';
import useCreateSavedSearch from 'views/logic/views/UseCreateSavedSearch';

type Props = {
  location: {
    query: { [string]: any },
  },
  params: {
    streamId: string,
  },
  route: {},
};

const StreamSearchPage = ({ params: { streamId }, route, location }: Props) => {
  const { query } = location;
  const newView = useCreateSavedSearch(streamId);

  const [loaded, HookComponent] = useLoadView(newView, query);

  if (HookComponent) {
    return <HookComponent />;
  }

  if (loaded) {
    return (
      <ViewLoaderContext.Provider value={loadView}>
        <NewViewLoaderContext.Provider value={loadNewView}>
          <ExtendedSearchPage route={route} />
        </NewViewLoaderContext.Provider>
      </ViewLoaderContext.Provider>
    );
  }

  return <Spinner />;
};

export default withParams(StreamSearchPage);
