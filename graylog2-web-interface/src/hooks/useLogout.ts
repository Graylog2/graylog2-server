import { useCallback } from 'react';

import { SessionActions } from 'stores/sessions/SessionStore';
import Routes from 'routing/Routes';
import useHistory from 'routing/useHistory';

const logout = () => SessionActions.logout()
  .then(() => SessionActions.validate);

const useLogout = () => {
  const history = useHistory();

  return useCallback(() => logout().then(() => history.push(Routes.STARTPAGE)), [history]);
};

export default useLogout;
