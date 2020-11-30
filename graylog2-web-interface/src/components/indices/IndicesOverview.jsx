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
import naturalSort from 'javascript-natural-sort';

import { Col, Row } from 'components/graylog';
import { ClosedIndexDetails, IndexDetails, IndexSummary } from 'components/indices';

class IndicesOverview extends React.Component {
  static propTypes = {
    closedIndices: PropTypes.array.isRequired,
    deflector: PropTypes.object.isRequired,
    indexDetails: PropTypes.object.isRequired,
    indices: PropTypes.object.isRequired,
    indexSetId: PropTypes.string.isRequired,
  };

  _formatIndex = (indexName, index) => {
    const indexSummary = this.props.indices[indexName];
    const indexRange = indexSummary && indexSummary.range ? indexSummary.range : null;

    return (
      <Row key={`index-summary-${indexName}`} className="content index-description">
        <Col md={12}>
          <IndexSummary index={index}
                        name={indexName}
                        count={indexSummary.size}
                        indexRange={indexRange}
                        isDeflector={indexSummary.is_deflector}>
            <span>
              <IndexDetails index={this.props.indexDetails[indexName]}
                            indexName={indexName}
                            indexRange={indexRange}
                            indexSetId={this.props.indexSetId}
                            isDeflector={indexSummary.is_deflector} />
            </span>
          </IndexSummary>
        </Col>
      </Row>
    );
  };

  _formatClosedIndex = (indexName, index) => {
    const indexRange = index.range;

    return (
      <Row key={`index-summary-${indexName}`} className="content index-description">
        <Col md={12}>
          <IndexSummary index={index} name={indexName} indexRange={indexRange} isDeflector={index.is_deflector}>
            <span>
              <ClosedIndexDetails indexName={indexName} indexRange={indexRange} />
            </span>
          </IndexSummary>
        </Col>
      </Row>
    );
  };

  render() {
    const indices = Object.keys(this.props.indices).map((indexName) => {
      return !this.props.indices[indexName].is_closed
        ? this._formatIndex(indexName, this.props.indices[indexName]) : this._formatClosedIndex(indexName, this.props.indices[indexName]);
    });

    return (
      <span>
        {indices.sort((index1, index2) => naturalSort(index2.key, index1.key))}
      </span>
    );
  }
}

export default IndicesOverview;
