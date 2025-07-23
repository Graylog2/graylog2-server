/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import * as React from 'react';
import { useCallback, useState, useEffect } from 'react';

import useLocation from 'routing/useLocation';
import ErrorPage from 'components/errors/ErrorPage';
import ErrorsActions from 'actions/errors/ErrorsActions';
import type { ReportedError } from 'logic/errors/ReportedErrors';
import {
  ReactErrorType,
  NotFoundErrorType,
  UnauthorizedErrorType,
  StreamPermissionErrorType,
} from 'logic/errors/ReportedErrors';
import RuntimeErrorPage from 'pages/RuntimeErrorPage';
import NotFoundPage from 'pages/NotFoundPage';
import UnauthorizedErrorPage from 'pages/UnauthorizedErrorPage';
import StreamPermissionErrorPage from 'pages/StreamPermissionErrorPage';
import useProductName from 'brand-customization/useProductName';

const FallbackErrorPage = ({ reportedError }: { reportedError: ReportedError }) => {
  const productName = useProductName();

  return (
    <ErrorPage
      title="Something went wrong"
      description={
        <p>
          An unknown error has occurred. Please have a look at the following message and the {productName} server log
          for more information.
        </p>
      }>
      <pre>{JSON.stringify(reportedError)}</pre>
    </ErrorPage>
  );
};

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
  children: React.ReactNode;
};

const ReportedErrorBoundary = ({ children }: Props) => {
  const [reportedError, setReportedError] = useState<ReportedError | undefined>();

  const report = useCallback((newError: ReportedError) => setReportedError(newError), []);

  useEffect(() => ErrorsActions.report.listen(report), [report]);

  const location = useLocation();

  useEffect(() => {
    setReportedError(null);
  }, [location]);

  return reportedError ? <ReportedErrorPage reportedError={reportedError} /> : <>{children}</>;
};

export default ReportedErrorBoundary;
