import React from 'react';
import PropTypes from 'prop-types';
import { Button, ButtonToolbar } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';

import Routes from 'routing/Routes';

class AlertsHeaderToolbar extends React.Component {
  static propTypes = {
    active: PropTypes.string.isRequired,
  };

  _isActive = (active, route) => {
    return active === route ? 'active' : '';
  };

  render() {
    const { active } = this.props;

    return (
      <ButtonToolbar>
        <LinkContainer to={Routes.ALERTS.LIST}>
          <Button bsStyle="info" className={this._isActive(active, Routes.ALERTS.LIST)}>Alerts</Button>
        </LinkContainer>
        <LinkContainer to={Routes.ALERTS.CONDITIONS}>
          <Button bsStyle="info" className={this._isActive(active, Routes.ALERTS.CONDITIONS)}>Conditions</Button>
        </LinkContainer>
        <LinkContainer to={Routes.ALERTS.NOTIFICATIONS}>
          <Button bsStyle="info" className={this._isActive(active, Routes.ALERTS.NOTIFICATIONS)}>Notifications</Button>
        </LinkContainer>
      </ButtonToolbar>
    );
  }
}

export default AlertsHeaderToolbar;
