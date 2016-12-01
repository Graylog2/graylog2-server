import React from 'react';
import { Alert, Col, DropdownButton, MenuItem } from 'react-bootstrap';

import { EntityListItem } from 'components/common';

const UnknownAlertNotification = React.createClass({
  propTypes: {
    alertNotification: React.PropTypes.object.isRequired,
    onDelete: React.PropTypes.func.isRequired,
  },

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
  },
});

export default UnknownAlertNotification;
