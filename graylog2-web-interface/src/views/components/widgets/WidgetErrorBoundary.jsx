// @flow strict
import * as React from 'react';
import WidgetFailed from './WidgetFailed';

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
    const { children } = this.props;
    return error
      ? <WidgetFailed error={error} />
      : children;
  }
}
