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
import { useContext, useMemo } from 'react';
import { useLocation, UNSAFE_DataRouterContext as DataRouterContext, matchRoutes } from 'react-router-dom';

import { singleton } from 'logic/singleton';

const useRoutePattern = () => {
  const location = useLocation();
  const dataRouterContext = useContext(DataRouterContext);

  return useMemo(() => {
    if (dataRouterContext?.router?.routes) {
      const { router: { routes } } = dataRouterContext;
      const matches = matchRoutes(routes, location.pathname);
      const { route } = matches.at(-1);

      return route.path;
    }

    return undefined;
  }, [location.pathname, dataRouterContext]);
};

export default singleton('hooks.useRoutePattern', () => useRoutePattern);
