// @flow strict
import * as React from 'react';
import { useState, useEffect } from 'react';
import { withRouter } from 'react-router';

import ErrorsActions from 'actions/errors/ErrorsActions';
import { type ReportedError as ReportedErrorType, ReactErrorType, UnauthoriedErrorType } from 'logic/errors/ReportedError';

import RuntimeErrorPage from 'pages/RuntimeErrorPage';
import UnauthorizedErrorPage from 'pages/UnauthorizedErrorPage';

const ReportedError = ({ children, router }) => {
  const [reportedError, setReportedError] = useState();

  const report = (newError: ReportedErrorType) => setReportedError(newError);

  useEffect(() => {
    ErrorsActions.report.listen(report);
    const unlistenRouter = router.listen(() => setReportedError(null));
    return () => unlistenRouter();
  }, []);

  if (reportedError && reportedError.type === ReactErrorType) {
    return <RuntimeErrorPage error={reportedError.error} componentStack={reportedError.componentStack} />;
  }

  if (reportedError && reportedError.type === UnauthoriedErrorType) {
    return <UnauthorizedErrorPage error={reportedError.error} />;
  }

  return children;
};


export default withRouter(ReportedError);
