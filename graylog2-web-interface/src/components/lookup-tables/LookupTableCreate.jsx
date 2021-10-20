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

import { Row, Col } from 'components/bootstrap';
import { LookupTableForm } from 'components/lookup-tables';

class LookupTableCreate extends React.Component {
  static propTypes = {
    saved: PropTypes.func.isRequired,
    validate: PropTypes.func,
    validationErrors: PropTypes.object,
  };

  static defaultProps = {
    validate: null,
    validationErrors: {},
  };

  state = {
    table: undefined,
  };

  render() {
    return (
      <div>
        <Row className="content">
          <Col lg={8}>
            <LookupTableForm saved={this.props.saved}
                             create
                             validate={this.props.validate}
                             validationErrors={this.props.validationErrors} />
          </Col>
        </Row>
      </div>
    );
  }
}

export default LookupTableCreate;
