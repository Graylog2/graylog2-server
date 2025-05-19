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
import type { ErrorInfo } from 'react';

import { createReactError } from 'logic/errors/ReportedErrors';
import ErrorsActions from 'actions/errors/ErrorsActions';

type Props = {
  children: React.ReactNode;
};

class RuntimeErrorBoundary extends React.Component<Props> {
  componentDidCatch(error: Error, info: ErrorInfo) {
    ErrorsActions.report(createReactError(error, { componentStack: info?.componentStack }));
  }

  render() {
    const { children } = this.props;

    return children;
  }
}

export default RuntimeErrorBoundary;
