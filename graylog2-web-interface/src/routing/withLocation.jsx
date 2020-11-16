/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
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
