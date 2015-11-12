import React from 'react';
import { Col, Row } from 'react-bootstrap';
import naturalSort from 'javascript-natural-sort';

import { ClosedIndexDetails, IndexDetails, IndexSummary } from 'components/indices';

const IndicesOverview = React.createClass({
  propTypes: {
    closedIndices: React.PropTypes.array.isRequired,
    deflector: React.PropTypes.object.isRequired,
    indexRanges: React.PropTypes.array.isRequired,
    indices: React.PropTypes.object.isRequired,
  },
  _isDeflector(index) {
    return index.name === this.props.deflector.info.current_target;
  },
  _formatIndex(indexName, index) {
    const indexRange = this.props.indexRanges.filter((indexRange) => indexRange.index_name === indexName)[0];
    index.name = indexName;
    return (
      <Row key={'index-summary-' + index.name} className="content index-description">
        <Col md={12}>
          <IndexSummary index={index} indexRange={indexRange} isDeflector={this._isDeflector(index)}>
            <span>
              <IndexDetails index={index} indexRange={indexRange} isDeflector={this._isDeflector(index)}/>
            </span>
          </IndexSummary>
        </Col>
      </Row>
    );
  },
  _formatClosedIndex(indexName, index) {
    const indexRange = this.props.indexRanges.filter((indexRange) => indexRange.index_name === indexName)[0];
    return (
      <Row key={'index-summary-' + indexName} className="content index-description">
        <Col md={12}>
          <IndexSummary index={index} indexRange={indexRange} isDeflector={this._isDeflector(index)}>
            <span>
              <ClosedIndexDetails indexRange={indexRange} />
            </span>
          </IndexSummary>
        </Col>
      </Row>
    );
  },
  render() {
    const indices = Object.keys(this.props.indices).map((indexName) => this._formatIndex(indexName, this.props.indices[indexName]));
    this.props.closedIndices.forEach((closedIndex) => indices.push(this._formatClosedIndex(closedIndex, { name: closedIndex })));
    return (
      <span>
        {indices.sort((index1, index2) => naturalSort(index2.key, index1.key))}
      </span>
    );
  },
});

export default IndicesOverview;
