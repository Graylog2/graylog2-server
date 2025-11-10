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

import TelemetryContext from 'logic/telemetry/TelemetryContext';

export type FallbackComponentType = React.ComponentType<{ error: Error; info: ErrorInfo }>;
type Props = {
  FallbackComponent: FallbackComponentType;
  children: React.ReactNode;
};

type State = {
  error?: Error;
  info?: ErrorInfo;
};

class ErrorBoundary extends React.Component<Props, State> {
  static contextType = TelemetryContext;

  context: React.ContextType<typeof TelemetryContext>;

  constructor(props: Props) {
    super(props);
    this.state = {};
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    this.setState({ error, info });
    this.context.sendErrorReport(error);
  }

  render() {
    const { error, info } = this.state;
    const { FallbackComponent, children } = this.props;

    if (error) {
      return <FallbackComponent error={error} info={info} />;
    }

    return children;
  }
}

export default ErrorBoundary;
