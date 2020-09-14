// @flow strict
import * as React from 'react';

import ErrorPage from 'components/errors/ErrorPage';

const UserHasNoStreamAccess = () => {
  return (
    <ErrorPage title="No stream permissions."
               description={'We cannot start a search right now, because you are not allowed to access any stream. '
                 + 'If you feel this is an error, please contact your administrator.'} />
  );
};

export default UserHasNoStreamAccess;
