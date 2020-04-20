// @flow strict
import * as React from 'react';
import { useState, useEffect } from 'react';
import { withRouter } from 'react-router';

import ErrorsActions from 'actions/errors/ErrorsActions';
import { type ReportedError as ReportedErrorType, ReactErrorType, UnauthoriedErrorType } from 'logic/errors/ReportedError';

import RuntimeErrorPage from 'pages/RuntimeErrorPage';
import UnauthorizedErrorPage from 'pages/UnauthorizedErrorPage';

const ReportedError = ({ children, router }) => {
  const [reportedError, setReportedError] = useState<?ReportedErrorType>();

  const report = (newError: ReportedErrorType) => setReportedError(newError);

  useEffect(() => {
    const unlistenErrorsReport = ErrorsActions.report.listen(report);
    const unlistenRouter = router.listen(() => { if (reportedError) setReportedError(null); });
    return () => {
      unlistenRouter();
      unlistenErrorsReport();
    };
  }, []);

  if (reportedError && reportedError.type === ReactErrorType) {
    return <RuntimeErrorPage error={reportedError.error} componentStack={reportedError.info.componentStack} />;
  }

  if (reportedError && reportedError.type === UnauthoriedErrorType) {
    return <UnauthorizedErrorPage error={reportedError.error} />;
  }

  return children;
};


export default withRouter(ReportedError);
