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

import { Col, Row } from 'components/bootstrap';
import { ClosedIndexDetails, IndexDetails, IndexSummary } from 'components/indices';
import type { IndexInfo } from 'stores/indices/IndicesStore';

const Index = ({ index, indexDetails, indexSetId }: { index: IndexSummary, indexDetails: Array<IndexInfo>, indexSetId: string }) => {
  const indexRange = index && index.range ? index.range : null;
  const details = indexDetails.find(({ index_name }) => index_name === index.index_name);

  return (
    <Row className="content index-description">
      <Col md={12}>
        <IndexSummary index={index}
                      name={index.index_name}
                      count={index.size}
                      indexRange={indexRange}
                      isDeflector={index.is_deflector}>
          <span>
            <IndexDetails index={details}
                          indexName={index.index_name}
                          indexRange={indexRange}
                          indexSetId={indexSetId}
                          isDeflector={index.is_deflector} />
          </span>
        </IndexSummary>
      </Col>
    </Row>
  );
};

const ClosedIndex = ({ index }: { index: IndexSummary }) => {
  const indexRange = index.range;

  return (
    <Row className="content index-description">
      <Col md={12}>
        <IndexSummary index={index} name={index.index_name} indexRange={indexRange} isDeflector={index.is_deflector}>
          <span>
            <ClosedIndexDetails indexName={index.index_name} indexRange={indexRange} />
          </span>
        </IndexSummary>
      </Col>
    </Row>
  );
};

type Props = {
  indexDetails: Array<IndexInfo>,
  indices: Array<IndexSummary>
  indexSetId: string,
}

const IndicesOverview = ({ indexDetails, indices, indexSetId }: Props) => (
  <span>
    {indices.map((index) => (!index.is_closed
      ? <Index index={index} indexDetails={indexDetails} indexSetId={indexSetId} key={`index-summary-${index.index_name}`} />
      : <ClosedIndex index={index} key={`index-summary-${index.index_name}`} />),
    )}
  </span>
);

IndicesOverview.propTypes = {
  indexDetails: PropTypes.array.isRequired,
  indices: PropTypes.array.isRequired,
  indexSetId: PropTypes.string.isRequired,
};

export default IndicesOverview;
