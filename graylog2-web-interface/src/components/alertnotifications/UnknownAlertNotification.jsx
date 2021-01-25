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
import PropTypes from 'prop-types';
import React from 'react';

import { Alert, Col, DropdownButton, MenuItem } from 'components/graylog';
import { EntityListItem } from 'components/common';

class UnknownAlertNotification extends React.Component {
  static propTypes = {
    alertNotification: PropTypes.object.isRequired,
    onDelete: PropTypes.func.isRequired,
  };

  render() {
    const notification = this.props.alertNotification;

    const actions = [
      <DropdownButton key="actions-button" title="Actions" pullRight id={`more-actions-dropdown-${notification.id}`}>
        <MenuItem onSelect={this.props.onDelete}>Delete</MenuItem>
      </DropdownButton>,
    ];

    const content = (
      <Col md={12}>
        <Alert bsStyle="warning">
          Could not resolve notification type. This is most likely caused by a missing plugin in your Graylog setup.
        </Alert>
      </Col>
    );

    return (
      <EntityListItem key={`entry-list-${notification.id}`}
                      title="Unknown notification"
                      titleSuffix={`(${notification.type})`}
                      description="Cannot be executed while the notification type is unknown"
                      actions={actions}
                      contentRow={content} />
    );
  }
}

export default UnknownAlertNotification;
