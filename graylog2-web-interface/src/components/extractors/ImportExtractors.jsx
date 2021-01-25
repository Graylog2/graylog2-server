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

import { Row, Col, Button } from 'components/graylog';
import { Input } from 'components/bootstrap';
import ActionsProvider from 'injection/ActionsProvider';
import UserNotification from 'util/UserNotification';

const ExtractorsActions = ActionsProvider.getActions('Extractors');

class ImportExtractors extends React.Component {
  static propTypes = {
    input: PropTypes.object.isRequired,
  };

  _onSubmit = (event) => {
    event.preventDefault();

    try {
      const parsedExtractors = JSON.parse(this.extractorsInput.getValue());
      const { extractors } = parsedExtractors;

      ExtractorsActions.import(this.props.input.id, extractors);
    } catch (error) {
      UserNotification.error(`There was an error while parsing extractors. Are they in JSON format? ${error}`,
        'Could not import extractors');
    }
  };

  render() {
    return (
      <Row className="content">
        <Col md={12}>
          <Row>
            <Col md={12}>
              <h2>Extractors JSON</h2>
            </Col>
          </Row>
          <Row>
            <Col md={12}>
              <form onSubmit={this._onSubmit}>
                <Input type="textarea" ref={(extractorsInput) => { this.extractorsInput = extractorsInput; }} id="extractor-export-textarea" rows={30} />
                <Button type="submit" bsStyle="success">Add extractors to input</Button>
              </form>
            </Col>
          </Row>
        </Col>
      </Row>
    );
  }
}

export default ImportExtractors;
