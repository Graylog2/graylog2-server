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
import React from 'react';
import PropTypes from 'prop-types';

import { LinkContainer } from 'components/graylog/router';
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
