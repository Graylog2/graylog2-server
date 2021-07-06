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
import { ErrorBoundary } from 'react-error-boundary';

import { Col } from 'components/graylog';

type ErrorFallbackProps = {
  error: {
    message: string,
  },
  title: string,
};

const ErrorFallback = ({ error, title }: ErrorFallbackProps) => (
  <>
    <h2>{title}</h2>
    <p>Something went wrong:</p>
    <pre>{error.message}</pre>
  </>
);

type BoundaryProps = {
  children: React.ReactNode,
  title: string,
}

const Boundary = ({ children, title }: BoundaryProps) => (
  <ErrorBoundary FallbackComponent={(props) => <ErrorFallback title={title} {...props} />}>
    {children}
  </ErrorBoundary>
);

const ConfigletContainer = ({ children, title }: BoundaryProps) => (
  <Col md={6}>
    <Boundary title={title}>
      {children}
    </Boundary>
  </Col>
);

export default ConfigletContainer;
