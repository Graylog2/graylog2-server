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

import { Table, Button } from 'components/graylog';
import { Icon } from 'components/common';

import styles from './CommonNotificationSummary.css';

class CommonNotificationSummary extends React.Component {
  static propTypes = {
    type: PropTypes.string.isRequired,
    notification: PropTypes.object.isRequired,
    definitionNotification: PropTypes.object.isRequired,
    children: PropTypes.element.isRequired,
  };

  state = {
    displayDetails: false,
  };

  toggleDisplayDetails = () => {
    const { displayDetails } = this.state;

    this.setState({ displayDetails: !displayDetails });
  };

  render() {
    const { type, notification, definitionNotification, children } = this.props;
    const { displayDetails } = this.state;

    return (
      <>
        <h4>{notification.title || definitionNotification.notification_id}</h4>
        <dl>
          <dd>{type}</dd>
          <dd>
            <Button bsStyle="link" className="btn-text" bsSize="xsmall" onClick={this.toggleDisplayDetails}>
              <Icon name={`caret-${displayDetails ? 'down' : 'right'}`} />&nbsp;
              {displayDetails ? 'Less details' : 'More details'}
            </Button>
            {displayDetails && (
            <Table condensed hover className={styles.fixedTable}>
              <tbody>
                <tr>
                  <td>Description</td>
                  <td>{notification.description || 'No description given'}</td>
                </tr>
                {children}
              </tbody>
            </Table>
            )}
          </dd>
        </dl>
      </>
    );
  }
}

export default CommonNotificationSummary;
