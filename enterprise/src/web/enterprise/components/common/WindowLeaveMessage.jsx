// @flow strict
import React from 'react';
import PropTypes from 'prop-types';
// $FlowFixMe: imports from core need to be fixed in flow
import { withRouter } from 'react-router';

// $FlowFixMe: imports from core need to be fixed in flow
import connect from 'stores/connect';
// $FlowFixMe: imports from core need to be fixed in flow
import AppConfig from 'util/AppConfig';

import { ViewStore } from 'enterprise/stores/ViewStore';

type Router = {
  setRouteLeaveHook: (any, () => ?string) => () => void,
};

type Props = {
  dirty: boolean,
  route: any,
  router: Router,
};

class WindowLeaveMessage extends React.PureComponent<Props> {
  componentDidMount() {
    window.addEventListener('beforeunload', this.handleLeavePage);
    this.unsubscribe = this.props.router.setRouteLeaveHook(this.props.route, this.routerWillLeave);
  }

  componentWillUnmount() {
    window.removeEventListener('beforeunload', this.handleLeavePage);
    this.unsubscribe();
  }

  unsubscribe: () => void;

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
