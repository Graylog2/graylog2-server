import React from 'react';
import PropTypes from 'prop-types';
import { LinkContainer } from 'react-router-bootstrap';

import { ButtonToolbar, Button } from 'components/graylog';
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
        <LinkContainer to={Routes.LEGACY_ALERTS.LIST}>
          <Button bsStyle="info" className={this._isActive(active, Routes.LEGACY_ALERTS.LIST)}>Alerts</Button>
        </LinkContainer>
        <LinkContainer to={Routes.LEGACY_ALERTS.CONDITIONS}>
          <Button bsStyle="info" className={this._isActive(active, Routes.LEGACY_ALERTS.CONDITIONS)}>Conditions</Button>
        </LinkContainer>
        <LinkContainer to={Routes.LEGACY_ALERTS.NOTIFICATIONS}>
          <Button bsStyle="info" className={this._isActive(active, Routes.LEGACY_ALERTS.NOTIFICATIONS)}>Notifications</Button>
        </LinkContainer>
      </ButtonToolbar>
    );
  }
}

export default AlertsHeaderToolbar;
