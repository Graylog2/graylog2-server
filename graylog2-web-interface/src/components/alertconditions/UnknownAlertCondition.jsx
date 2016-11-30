import React from 'react';
import { Alert, Col, DropdownButton, MenuItem } from 'react-bootstrap';

import { EntityListItem } from 'components/common';

const UnknownAlertCondition = React.createClass({
  propTypes: {
    alertCondition: React.PropTypes.object.isRequired,
    stream: React.PropTypes.object,
    onDelete: React.PropTypes.func.isRequired,
  },

  render() {
    const condition = this.props.alertCondition;
    const stream = this.props.stream;

    const actions = [
      <DropdownButton key="actions-button" title="Actions" pullRight id={`more-actions-dropdown-${condition.id}`}>
        <MenuItem onSelect={this.props.onDelete}>Delete</MenuItem>
      </DropdownButton>,
    ];

    const content = (
      <Col md={12}>
        <Alert bsStyle="warning">
          Could not resolve condition type. This is most likely caused by a missing plugin in your Graylog setup.
        </Alert>
      </Col>
    );
    return (
      <EntityListItem key={`entry-list-${condition.id}`}
                      title="Unknown condition"
                      titleSuffix={`(${condition.type})`}
                      description={stream ? <span>Watching stream <em>{stream.title}</em></span> : 'Not watching any stream'}
                      actions={actions}
                      contentRow={content} />
    );
  },
});

export default UnknownAlertCondition;
