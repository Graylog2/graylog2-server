import React from 'react';
import { Col, Row } from 'react-bootstrap';
import naturalSort from 'javascript-natural-sort';

import { ClosedIndexDetails, IndexDetails, IndexSummary } from 'components/indices';

const IndicesOverview = React.createClass({
  propTypes: {
    closedIndices: React.PropTypes.array.isRequired,
    deflector: React.PropTypes.object.isRequired,
    indexDetails: React.PropTypes.object.isRequired,
    indices: React.PropTypes.object.isRequired,
    indexSetId: React.PropTypes.string.isRequired,
  },
  _formatIndex(indexName, index) {
    const indexSummary = this.props.indices[indexName];
    const indexRange = indexSummary && indexSummary.range ? indexSummary.range : null;
    return (
      <Row key={`index-summary-${indexName}`} className="content index-description">
        <Col md={12}>
          <IndexSummary index={index} name={indexName} count={indexSummary.size}
                        indexRange={indexRange} isDeflector={indexSummary.is_deflector}>
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
  },
  _formatClosedIndex(indexName, index) {
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
  },
  render() {
    const indices = Object.keys(this.props.indices).map((indexName) => {
      return !this.props.indices[indexName].is_closed ?
        this._formatIndex(indexName, this.props.indices[indexName]) : this._formatClosedIndex(indexName, this.props.indices[indexName]);
    });
    return (
      <span>
        {indices.sort((index1, index2) => naturalSort(index2.key, index1.key))}
      </span>
    );
  },
});

export default IndicesOverview;
