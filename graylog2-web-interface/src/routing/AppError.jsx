// @flow strict
import * as React from 'react';
import { useState, useEffect } from 'react';
import { withRouter } from 'react-router';

import ErrorsActions from 'actions/errors/ErrorsActions';
import AppErrorClass from 'logic/errors/AppError';
import RuntimeErrorPage from 'pages/RuntimeErrorPage';

const AppError = ({ children, router }) => {
  const [appError, setAppError] = useState();

  const displayError = (newError: AppErrorClass) => setAppError(newError);

  useEffect(() => {
    ErrorsActions.displayError.listen(displayError);
    const unlistenRouter = router.listen(() => setAppError(null));
    return () => unlistenRouter();
  }, []);

  if (appError && appError.type === AppErrorClass.Type.Runtime) {
    return <RuntimeErrorPage error={appError.error} componentStack={appError.componentStack} />;
  }

  return children;
};


export default withRouter(AppError);
