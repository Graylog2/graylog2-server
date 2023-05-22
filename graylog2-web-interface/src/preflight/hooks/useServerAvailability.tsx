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

import { qualifyUrl } from 'util/URLUtils';
import { Builder } from 'logic/rest/FetchProvider';

const fetchServerAvailability = () => new Builder('GET', qualifyUrl('/api'))
  .json()
  .setHeader('X-Graylog-No-Session-Extension', 'true')
  .build();

const useServerAvailability = () => {
  const { data } = useQuery(
    ['server-availability'],
    fetchServerAvailability,
    { refetchInterval: 2000 },
  );

  return { data: !!data };
};

export default useServerAvailability;
