// @flow strict
import * as React from 'react';
import { useState, useEffect } from 'react';
import { withRouter } from 'react-router';

import ErrorsActions from 'actions/errors/ErrorsActions';
import { type ReportedError as ReportedErrorType, ReactErrorType, UnauthorizedErrorType } from 'logic/errors/ReportedErrors';

import RuntimeErrorPage from 'pages/RuntimeErrorPage';
import UnauthorizedErrorPage from 'pages/UnauthorizedErrorPage';

const ReportedError = ({ children, router }) => {
  const [reportedError, setReportedError] = useState<?ReportedErrorType>();

  const report = (newError: ReportedErrorType) => setReportedError(newError);

  useEffect(() => {
    const unlistenErrorsReport = ErrorsActions.report.listen(report);
    return () => {
      unlistenErrorsReport();
    };
  }, []);

  useEffect(() => {
    const unlistenRouter = router.listen(() => reportedError && setReportedError(null));
    return () => {
      unlistenRouter();
    };
  }, [reportedError]);

  if (reportedError && reportedError.type === ReactErrorType) {
    return <RuntimeErrorPage error={reportedError.error} componentStack={reportedError.info.componentStack} />;
  }

  if (reportedError && reportedError.type === UnauthorizedErrorType) {
    return <UnauthorizedErrorPage error={reportedError.error} />;
  }

  return children;
};


export default withRouter(ReportedError);
