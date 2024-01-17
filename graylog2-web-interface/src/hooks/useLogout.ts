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
import { useCallback, useMemo } from 'react';

import { SessionActions } from 'stores/sessions/SessionStore';
import Routes from 'routing/Routes';
import useHistory from 'routing/useHistory';
import usePluginEntities from 'hooks/usePluginEntities';

const logout = () => SessionActions.logout()
  .then(() => SessionActions.validate);

const wrapHooks = (hooks: Array<() => void | Promise<unknown>>) => () => Promise.allSettled(hooks
  .map(async (hook) => {
    try {
      await hook();
    } catch (e) {
      // eslint-disable-next-line no-console
      console.error('Error in logout hook: ', e);
    }
  }),
);

const useLogout = () => {
  const history = useHistory();
  const logoutHooks = usePluginEntities('hooks.logout');
  const logoutHook = useMemo(() => wrapHooks(logoutHooks), [logoutHooks]);

  return useCallback(() => logoutHook().then(logout).then(() => history.push(Routes.STARTPAGE)), [history, logoutHook]);
};

export default useLogout;
