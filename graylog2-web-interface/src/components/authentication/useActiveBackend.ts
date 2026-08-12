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

import { useQuery } from '@tanstack/react-query';

import { loadActiveAuthBackend, AUTHENTICATION_QUERY_KEY } from 'hooks/useAuthentication';

const useActiveBackend = () => {
  const { data, isSuccess } = useQuery({
    queryKey: [...AUTHENTICATION_QUERY_KEY, 'active'],
    queryFn: loadActiveAuthBackend,
  });

  return {
    finishedLoading: isSuccess,
    activeBackend: data?.backend,
    backendsTotal: data?.context?.backendsTotal,
  };
};

export default useActiveBackend;
