// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';

import ReportedError from 'logic/errors/ReportedError';
import ErrorsActions from 'actions/errors/ErrorsActions';

type Props = {
  children: React.Node,
};

class RuntimeErrorBoundary extends React.Component<Props> {
  static propTypes = {
    children: PropTypes.node,
  };

  static defaultProps = {
    children: null,
  };

  componentDidCatch(error: Error, info: { componentStack: string }) {
    ErrorsActions.report(ReportedError.createReactError(error, info));
  }

  render() {
    const { children } = this.props;
    return children;
  }
}

export default RuntimeErrorBoundary;
