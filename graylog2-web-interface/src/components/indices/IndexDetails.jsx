import React from 'react';
import { Button, Col, Row } from 'react-bootstrap';

import { IndicesActions, IndexRangesActions } from 'actions/indices';

import { IndexRangeSummary, ShardMeter, ShardRoutingOverview } from 'components/indices';

const IndexDetails = React.createClass({
  propTypes: {
    index: React.PropTypes.object.isRequired,
    indexRange: React.PropTypes.object.isRequired,
    isDeflector: React.PropTypes.bool.isRequired,
  },
  _formatActionButtons() {
    if (this.props.isDeflector) {
      return (
        <span>
          <Button bsStyle="warning" bsSize="xs" disabled>Deflector index cannot be closed</Button>{' '}
          <Button bsStyle="danger" bsSize="xs" disabled>Deflector index cannot be deleted</Button>
        </span>
      );
    }

    return (
      <span>
        <Button bsStyle="warning" bsSize="xs" onClick={this._onRecalculateIndex}>Recalculate index ranges</Button>{' '}
        <Button bsStyle="warning" bsSize="xs" onClick={this._onCloseIndex}>Close index</Button>{' '}
        <Button bsStyle="danger" bsSize="xs" onClick={this._onDeleteIndex}>Delete index</Button>
      </span>
    );
  },
  _onRecalculateIndex() {
    if (window.confirm('Really recalculate the index ranges for index ' + this.props.index.name + '?')) {
      IndexRangesActions.recalculateIndex(this.props.index.name);
    }
  },
  _onCloseIndex() {
    if (window.confirm('Really close index ' + this.props.index.name + '?')) {
      IndicesActions.close(this.props.index.name);
    }
  },
  _onDeleteIndex() {
    if (window.confirm('Really delete index ' + this.props.index.name + '?')) {
      IndicesActions.delete(this.props.index.name);
    }
  },
  render() {
    const { index, indexRange } = this.props;
    return (
      <div className="index-info">
        <IndexRangeSummary indexRange={indexRange} />{' '}

        {index.all_shards.segments} segments,{' '}
        {index.all_shards.open_search_contexts} open search contexts,{' '}
        {index.all_shards.documents.deleted} deleted messages

        <Row style={{marginBottom: '10'}}>
          <Col md={4} className="shard-meters">
            <ShardMeter title="Primary shard operations" shardMeter={index.primary_shards} />
          </Col>
          <Col md={4} className="shard-meters">
            <ShardMeter title="Total shard operations" shardMeter={index.all_shards} />
          </Col>
        </Row>

        <ShardRoutingOverview routing={index.routing} />

        <hr style={{marginBottom: '5', marginTop: '10'}}/>

        {this._formatActionButtons()}
      </div>
    );
  },
});

export default IndexDetails;
