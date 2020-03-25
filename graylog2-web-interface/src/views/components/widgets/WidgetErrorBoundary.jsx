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
    const childrenWithProps = React.Children.map(children, child => React.cloneElement(child, rest));
    return error
      ? <ErrorWidget title="While rendering this widget, the following error occurred:" errors={[{ description: error.toString() }]} />
      : childrenWithProps;
  }
}
