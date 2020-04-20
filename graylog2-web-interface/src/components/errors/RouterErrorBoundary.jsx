import React from 'react';
import PropTypes from 'prop-types';
import RuntimeErrorPage from 'pages/RuntimeErrorPage';

class RouterErrorBoundary extends React.Component {
  static propTypes = {
    children: PropTypes.node,
  };

  static defaultProps = {
    children: null,
  };

  constructor(props) {
    super(props);
    this.state = {};
  }

  componentDidCatch(error, info) {
    this.setState({ error, info });
  }

  render() {
    const { error, info } = this.state;
    const { children } = this.props;
    if (error) {
      return <RuntimeErrorPage error={error} componentStack={info.componentStack} />;
    }
    return children;
  }
}

export default RouterErrorBoundary;
