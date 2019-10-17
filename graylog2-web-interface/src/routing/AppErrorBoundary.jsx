import React from 'react';
import { withRouter } from 'react-router';
import PropTypes from 'prop-types';
import ErrorPage from '../pages/ErrorPage';

class AppErrorBoundary extends React.Component {
  static propTypes = {
    children: PropTypes.node,
    router: PropTypes.object.isRequired,
  };

  static defaultProps = {
    children: null,
  };

  constructor(props) {
    super(props);
    this.state = {};
    this.unlisten = () => {};
  }

  componentDidMount() {
    const { router } = this.props;
    this.unlisten = router.listen(() => this.setState(() => ({ error: undefined, info: undefined })));
  }

  componentWillUnmount() {
    this.unlisten();
  }

  componentDidCatch(error, info) {
    this.setState({ error, info });
  }

  render() {
    const { error, info } = this.state;
    const { children } = this.props;
    if (error) {
      return <ErrorPage error={error} info={info} />;
    }
    return children;
  }
}

export default withRouter(AppErrorBoundary);
