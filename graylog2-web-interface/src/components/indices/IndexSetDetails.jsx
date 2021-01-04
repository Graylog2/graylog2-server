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

import { Col } from 'components/graylog';
import { IndicesConfiguration } from 'components/indices';

import StyledIndexSetDetailsRow from './StyledIndexSetDetailsRow';

const IndexSetDetails = ({ indexSet }) => {
  return (
    <StyledIndexSetDetailsRow>
      <Col lg={3}>
        <dl>
          <dt>Index prefix:</dt>
          <dd>{indexSet.index_prefix}</dd>

          <dt>Shards:</dt>
          <dd>{indexSet.shards}</dd>

          <dt>Replicas:</dt>
          <dd>{indexSet.replicas}</dd>

          <dt>Field type refresh interval:</dt>
          <dd>{indexSet.field_type_refresh_interval / 1000.0} seconds</dd>
        </dl>
      </Col>

      <Col lg={6}>
        <IndicesConfiguration indexSet={indexSet} />
      </Col>
    </StyledIndexSetDetailsRow>
  );
};

IndexSetDetails.propTypes = {
  indexSet: PropTypes.object.isRequired,
};

export default IndexSetDetails;
