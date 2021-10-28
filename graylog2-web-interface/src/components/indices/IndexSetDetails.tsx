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
import { Col } from 'components/bootstrap';
import { IndicesConfiguration } from 'components/indices';
import type { IndexSet } from 'stores/indices/IndexSetsStore';

import StyledIndexSetDetailsRow from './StyledIndexSetDetailsRow';

type Props = {
  indexSet: IndexSet,
};

const IndexSetDetails = ({ indexSet }: Props) => {
  return (
    <StyledIndexSetDetailsRow>
      <Col lg={3}>
        <dl>
          <dt>Index prefix:</dt>
          <dd>{indexSet.index_prefix}</dd>
          <HideOnCloud>
            <dt>Shards:</dt>
            <dd>{indexSet.shards}</dd>
            <dt>Replicas:</dt>
            <dd>{indexSet.replicas}</dd>
          </HideOnCloud>
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

IndexSetDetails.propTypes = { indexSet: PropTypes.object.isRequired };
export default IndexSetDetails;
