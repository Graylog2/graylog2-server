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
