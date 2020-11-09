// @flow strict
import * as React from 'react';
import { useMemo } from 'react';
import { useLocation } from 'react-router-dom';

import useQuery from './useQuery';

export type Location = {
  query: { [string]: ?mixed },
  pathname: string,
  search: string,
};

type LocationContext = {
  location: Location,
};

const withLocation = <Props: LocationContext & {...}, ComponentType: React$ComponentType<Props>>(
  Component: ComponentType,
): React$ComponentType<$Diff<React$ElementConfig<ComponentType>, LocationContext>> => (props) => {
    const location = useLocation();
    const query = useQuery();
    const locationWithQuery: Location = useMemo(() => ({ ...location, query }), [location, query]);

    return <Component {...props} location={locationWithQuery} />;
  };

export default withLocation;
