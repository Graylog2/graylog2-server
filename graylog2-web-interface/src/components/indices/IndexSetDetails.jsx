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
// @flow strict
import PropTypes from 'prop-types';
import React from 'react';
import styled, { type StyledComponent } from 'styled-components';

import HideOnCloud from 'util/conditional/HideOnCloud';
import { Row, Col } from 'components/graylog';
import { IndicesConfiguration } from 'components/indices';
import type { IndexSet } from 'stores/indices/IndexSetsStore';

const StyledRow: StyledComponent<{}, void, Row> = styled(Row)`
  dl {
    margin-bottom: 0;
  }
  
  dt {
    float: left;
    width: 160px;
    overflow: hidden;
    clear: left;
    text-align: left;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  
  dd {
    margin-left: 180px;
  }
`;

type Props = {
  indexSet: IndexSet,
};

const IndexSetDetails = ({ indexSet }: Props) => {
  return (
    <StyledRow>
      <Col md={3}>
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

      <Col md={6}>
        <IndicesConfiguration indexSet={indexSet} />
      </Col>
    </StyledRow>
  );
};

IndexSetDetails.propTypes = { indexSet: PropTypes.object.isRequired };
export default IndexSetDetails;
