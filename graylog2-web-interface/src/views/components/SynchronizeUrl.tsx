import * as React from 'react';
import { useEffect } from 'react';

import withLocation, { Location } from 'routing/withLocation';
import { useSyncWithQueryParameters } from 'views/hooks/SyncWithQueryParameters';
import { ViewStore } from 'views/stores/ViewStore';
import bindSearchParamsFromQuery from 'views/hooks/BindSearchParamsFromQuery';

const useBindSearchParamsFromQuery = (query: { [key: string]: unknown }) => {
  useEffect(() => {
    const { view } = ViewStore.getInitialState();

    bindSearchParamsFromQuery({ view, query, retry: () => Promise.resolve() });
  }, [query]);
};

type Props = {
  location: Location,
};

const SynchronizeUrl = ({ location }: Props) => {
  const { pathname, search } = location;
  const query = `${pathname}${search}`;
  useBindSearchParamsFromQuery(location.query);
  useSyncWithQueryParameters(query);

  return <></>;
};

export default withLocation(SynchronizeUrl);
