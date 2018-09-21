import React from 'react';
import PropTypes from 'prop-types';
import { withRouter } from 'react-router';

import connect from 'stores/connect';
import AppConfig from 'util/AppConfig';
import { ViewStore } from 'enterprise/stores/ViewStore';

class WindowLeaveMessage extends React.PureComponent {
  componentDidMount() {
    window.addEventListener('beforeunload', this.handleLeavePage);
    this.props.router.setRouteLeaveHook(this.props.route, this.routerWillLeave);
  }

  componentWillUnmount() {
    window.removeEventListener('beforeunload', this.handleLeavePage);
  }

  routerWillLeave = () => {
    return this.handleLeavePage({});
  };

  handleLeavePage = (e) => {
    if (AppConfig.gl2DevMode()) {
      return null;
    }
    const dirty = this.props.dirty || false;
    if (dirty) {
      const question = 'Are you sure you want to leave the page? Any unsaved changes will be lost.';
      e.returnValue = question;
      return question;
    }
    return null;
  };

  render() {
    return null;
  }
}
WindowLeaveMessage.propTypes = {
  dirty: PropTypes.bool.isRequired,
  route: PropTypes.object.isRequired,
  router: PropTypes.shape({
    setRouteLeaveHook: PropTypes.func.isRequired,
  }).isRequired,
};

export default connect(withRouter(WindowLeaveMessage), { view: ViewStore }, ({ view }) => ({ dirty: view.dirty }));
