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

import { Alert, Col, Row } from 'components/graylog';

import commonStyles from '../common/commonStyles.css';

class EventDefinitionValidationSummary extends React.Component {
  static propTypes = {
    validation: PropTypes.object.isRequired,
  };

  render() {
    const { validation = {} } = this.props;
    const fieldsWithErrors = Object.keys(validation.errors);

    if (fieldsWithErrors.length === 0) {
      return null;
    }

    return (
      <Row>
        <Col md={12}>
          <Alert bsStyle="danger" className={commonStyles.validationSummary}>
            <h4>We found some errors!</h4>
            <p>Please correct the following errors before saving this Event Definition:</p>
            <ul>
              {fieldsWithErrors.map((field) => {
                return validation.errors[field].map((error) => {
                  const effectiveError = (field === 'config' ? error.replace('config', 'condition') : error);

                  return <li key={`${field}-${effectiveError}`}>{effectiveError}</li>;
                });
              })}
            </ul>
          </Alert>
        </Col>
      </Row>
    );
  }
}

export default EventDefinitionValidationSummary;
