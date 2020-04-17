// @flow strict
import * as React from 'react';
import { useState, useEffect } from 'react';
import { withRouter } from 'react-router';

import ErrorsActions from 'actions/errors/ErrorsActions';
import AppErrorClass from 'logic/errors/AppError';
import RuntimeErrorPage from 'pages/RuntimeErrorPage';
import UnauthorizedErrorPage from 'pages/UnauthorizedErrorPage';

const AppError = ({ children, router }) => {
  const [appError, setAppError] = useState();

  const report = (newError: AppErrorClass) => setAppError(newError);

  useEffect(() => {
    ErrorsActions.report.listen(report);
    const unlistenRouter = router.listen(() => setAppError(null));
    return () => unlistenRouter();
  }, []);

  if (appError && appError.type === AppErrorClass.Type.Runtime) {
    return <RuntimeErrorPage error={appError.error} componentStack={appError.componentStack} />;
  }

  if (appError && appError.type === AppErrorClass.Type.Unauthorized) {
    return <UnauthorizedErrorPage error={appError.error} componentStack={appError.componentStack} />;
  }

  return children;
};


export default withRouter(AppError);
