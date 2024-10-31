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

import React from 'react';

import type { ReportedError } from 'logic/errors/ReportedErrors';
import { createReactError } from 'logic/errors/ReportedErrors';
import { Section } from 'preflight/components/common';

type Props = {
  children: React.ReactNode | Array<React.ReactNode>
}

type State = {
  error: ReportedError | undefined,
}

class ErrorBoundary extends React.Component<Props, State> {
  constructor(props) {
    super(props);

    this.state = {
      error: undefined,
    };
  }

  static getDerivedStateFromError(error: Error, info: { componentStack: string }) {
    return { error: createReactError(error, info) };
  }

  render() {
    const { error } = this.state;
    const { children } = this.props;

    if (error) {
      return (
        <div>
          <Section title="Something went wrong" titleOrder={1}>
            <p>An unknown error has occurred. Please have a look at the following message and the graylog server log for more information.</p>
            <pre className="content">
              {error.error.message}
              <br />
              <br />
              {error.error.stack}
            </pre>
          </Section>
        </div>
      );
    }

    return children;
  }
}

export default ErrorBoundary;
