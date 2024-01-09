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
import { useCallback } from 'react';
import { useMutation } from '@tanstack/react-query';

import type FetchError from 'logic/errors/FetchError';
import { SessionActions } from 'stores/sessions/SessionStore';

const performLogin = ([username, password, host]: [string, string, string]) => SessionActions.login(username, password, host);

const useLogin = (onErrorChange: (message?: string) => void) => {
  const { mutateAsync, isLoading } = useMutation(performLogin,
    {
      onError: (error: FetchError) => {
        if (error.additional.status === 401) {
          onErrorChange('Invalid credentials, please verify them and retry.');
        } else {
          onErrorChange(`Error - the server returned: ${error.additional.status} - ${error.message}`);
        }
      },
    });

  const login = useCallback((username: string, password: string, location: string) => mutateAsync([username, password, location]), [mutateAsync]);

  return { login, isLoading };
};

export default useLogin;
