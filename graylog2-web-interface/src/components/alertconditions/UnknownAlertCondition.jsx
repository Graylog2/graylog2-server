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

class UnknownAlertCondition extends React.Component {
  static propTypes = {
    alertCondition: PropTypes.object.isRequired,
    stream: PropTypes.object,
    onDelete: PropTypes.func.isRequired,
  };

  render() {
    const condition = this.props.alertCondition;
    const { stream } = this.props;

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
  }
}

export default UnknownAlertCondition;
