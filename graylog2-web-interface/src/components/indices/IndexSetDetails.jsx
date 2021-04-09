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

import HideOnCloud from 'util/conditional/HideOnCloud';
import { Row, Col } from 'components/graylog';
import { IndicesConfiguration } from 'components/indices';

const style = require('!style/useable!css!./IndexSetDetails.css');

class IndexSetDetails extends React.Component {
  static propTypes = {
    indexSet: PropTypes.object.isRequired,
  };

  componentDidMount() {
    style.use();
  }

  componentWillUnmount() {
    style.unuse();
  }

  render() {
    const { indexSet } = this.props;

    return (
      <Row className="index-set-details">
        <Col md={3}>
          <dl>
            <dt>Index prefix:</dt>
            <dd>{indexSet.index_prefix}</dd>

            <dt>Shards:</dt>
            <dd>{indexSet.shards}</dd>
            <HideOnCloud>
              <dt>Replicas:</dt>
              <dd>{indexSet.replicas}</dd>
            </HideOnCloud>
            <dt>Field type refresh interval:</dt>
            <dd>{indexSet.field_type_refresh_interval / 1000.0} seconds</dd>
          </dl>
        </Col>

        <Col md={6}>
          <IndicesConfiguration indexSet={indexSet} />
        </Col>
      </Row>
    );
  }
}

export default IndexSetDetails;
