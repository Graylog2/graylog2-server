// @flow strict
import * as React from 'react';
import { useState, useEffect } from 'react';
import { useLocation } from 'react-router-dom';

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

type Props = {
  children: React.Node,
};

const ReportedErrorBoundary = ({ children }: Props) => {
  const [reportedError, setReportedError] = useState<?ReportedError>();

  const report = (newError: ReportedError) => setReportedError(newError);

  useEffect(() => {
    const unlistenErrorsReport = ErrorsActions.report.listen(report);

    return () => {
      unlistenErrorsReport();
    };
  }, []);

  const location = useLocation();

  useEffect(() => {
    if (reportedError) {
      setReportedError(null);
    }
  },
  // eslint-disable-next-line react-hooks/exhaustive-deps
  [location]);

  if (reportedError) {
    return <ReportedErrorPage reportedError={reportedError} />;
  }

  return children;
};

export default ReportedErrorBoundary;
