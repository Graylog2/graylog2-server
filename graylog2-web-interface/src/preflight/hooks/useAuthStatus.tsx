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

export const setBasicAuth = async (username: string, password: string) => {
  try {
    const response = await fetch('/api/data_nodes', {
      method: 'GET',
      headers: {
        Authorization: `Basic ${btoa(`${username}:${password}`)}`,
      },
    });

    if (response.ok) {
      return true;
    }

    return false;
  } catch (error) {
    return false;
  }
};

const fetchAuthStatus = () => fetch('/api/data_nodes');

const useAuthStatus = (): boolean => {
  const { data, error, isFetching } = useQuery(
    ['auth-status'],
    fetchAuthStatus,
  );

  return !!data && !error && !isFetching;
};

export default useAuthStatus;
