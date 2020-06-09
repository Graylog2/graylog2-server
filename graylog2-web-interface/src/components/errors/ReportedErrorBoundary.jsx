// @flow strict
import * as React from 'react';
import { useState, useEffect } from 'react';
import { withRouter } from 'react-router';

import ErrorPage from 'components/errors/ErrorPage';
import ErrorsActions from 'actions/errors/ErrorsActions';
import { type ReportedError, ReactErrorType, NotFoundErrorType, UnauthorizedErrorType, StreamPermissionErrorType } from 'logic/errors/ReportedErrors';

import RuntimeErrorPage from 'pages/RuntimeErrorPage';
import NotFoundPage from 'pages/NotFoundPage';
import UnauthorizedErrorPage from 'pages/UnauthorizedErrorPage';
import StreamPermissionErrorPage from 'pages/StreamPermissionErrorPage';

const FallbackErrorPage = ({ reportedError }: { reportedError: ReportedError }) => (
  <ErrorPage title="Something went wrong"
             description={<p>An unkown error has occured. Please have a look at the following message and the graylog server log for more information.</p>}>
    <pre>
      {JSON.stringify(reportedError)}
    </pre>
  </ErrorPage>
);

const ReportedErrorPage = ({ reportedError }: { reportedError: ReportedError }) => {
  switch (reportedError.type) {
    case ReactErrorType:
      return <RuntimeErrorPage error={reportedError.error} componentStack={reportedError.info.componentStack} />;
    case NotFoundErrorType:
      return <NotFoundPage />;
    case UnauthorizedErrorType:
      return <UnauthorizedErrorPage error={reportedError.error} />;
    case StreamPermissionErrorType:
      return <StreamPermissionErrorPage error={reportedError.error} />;
    default:
      return <FallbackErrorPage reportedError={reportedError} />;
  }
};

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

  if (reportedError) {
    return <ReportedErrorPage reportedError={reportedError} />;
  }

  return children;
};


export default withRouter(ReportedErrorBoundary);
