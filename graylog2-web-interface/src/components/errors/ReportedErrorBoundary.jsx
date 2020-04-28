// @flow strict
import * as React from 'react';
import { useState, useEffect } from 'react';
import { withRouter } from 'react-router';

import ErrorsActions from 'actions/errors/ErrorsActions';
import { type ReportedError, ReactErrorType, UnauthorizedErrorType, StreamPermissionErrorType } from 'logic/errors/ReportedErrors';

import RuntimeErrorPage from 'pages/RuntimeErrorPage';
import UnauthorizedErrorPage from 'pages/UnauthorizedErrorPage';
import StreamPermissionErrorPage from 'pages/StreamPermissionErrorPage';

const ReportedErrorBoundary = ({ children, router }) => {
  const [reportedError, setReportedError] = useState<?ReportedError>();

  const report = (newError: ReportedError) => setReportedError(newError);

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

  if (reportedError && reportedError.type === StreamPermissionErrorType) {
    return <StreamPermissionErrorPage error={reportedError.error} />;
  }

  return children;
};


export default withRouter(ReportedErrorBoundary);
