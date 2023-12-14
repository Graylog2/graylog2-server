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
