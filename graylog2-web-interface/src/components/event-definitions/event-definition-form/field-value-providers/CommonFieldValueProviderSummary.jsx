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

import styles from './CommonFieldValueProviderSummary.css';

class CommonFieldValueProviderSummary extends React.Component {
  static propTypes = {
    fieldName: PropTypes.string.isRequired,
    config: PropTypes.object.isRequired,
    keys: PropTypes.array.isRequired,
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
    const { fieldName, config, keys, children } = this.props;
    const { displayDetails } = this.state;

    return (
      <dl className={styles.field}>
        <dt>{fieldName}</dt>
        <dd>
          <Button bsStyle="link" className="btn-text" bsSize="xsmall" onClick={this.toggleDisplayDetails}>
            <Icon name={`caret-${displayDetails ? 'down' : 'right'}`} />&nbsp;
            {displayDetails ? 'Less details' : 'More details'}
          </Button>
          {displayDetails && (
            <Table condensed hover className={styles.fixedTable}>
              <tbody>
                <tr>
                  <td>Is Key?</td>
                  <td>{keys.includes(fieldName) ? 'Yes' : 'No'}</td>
                </tr>
                <tr>
                  <td>Data Type</td>
                  <td>{config.data_type}</td>
                </tr>
                {children}
              </tbody>
            </Table>
          )}
        </dd>
      </dl>
    );
  }
}

export default CommonFieldValueProviderSummary;
