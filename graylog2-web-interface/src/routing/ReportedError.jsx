// @flow strict
import * as React from 'react';
import { useState, useEffect } from 'react';
import { withRouter } from 'react-router';

import ErrorsActions from 'actions/errors/ErrorsActions';
import AppErrorClass from 'logic/errors/AppError';

import RuntimeErrorPage from 'pages/RuntimeErrorPage';
import UnauthorizedErrorPage from 'pages/UnauthorizedErrorPage';

const ReportedError = ({ children, router }) => {
  const [reportedError, setReportedError] = useState();

  const report = (newError: AppErrorClass) => setReportedError(newError);

  useEffect(() => {
    ErrorsActions.report.listen(report);
    const unlistenRouter = router.listen(() => setReportedError(null));
    return () => unlistenRouter();
  }, []);

  if (reportedError && reportedError.type === AppErrorClass.Type.Runtime) {
    return <RuntimeErrorPage error={reportedError.error} componentStack={reportedError.componentStack} />;
  }

  if (reportedError && reportedError.type === AppErrorClass.Type.Unauthorized) {
    return <UnauthorizedErrorPage error={reportedError.error} componentStack={reportedError.componentStack} />;
  }

  return children;
};


export default withRouter(ReportedError);
