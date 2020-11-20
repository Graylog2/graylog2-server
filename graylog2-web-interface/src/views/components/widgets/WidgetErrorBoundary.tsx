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
// @flow strict
import * as React from 'react';

import ErrorWidget from './ErrorWidget';

type State = {
  error?: Error,
};

type Props = {
  children: React.Node,
};

export default class WidgetErrorBoundary extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {};
  }

  static getDerivedStateFromError(error: Error) {
    return { error };
  }

  render() {
    const { error } = this.state;
    const { children, ...rest } = this.props;
    const childrenWithProps = React.Children.map(children, (child) => React.cloneElement(child, rest));

    return error
      ? <ErrorWidget title="While rendering this widget, the following error occurred:" errors={[{ description: error.toString() }]} />
      : childrenWithProps;
  }
}
