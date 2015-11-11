import React from 'react';
import moment from 'moment';
import { Col, Row } from 'react-bootstrap';

import { ShardMeter } from 'components/indices';

const IndexDetails = React.createClass({
  propTypes: {
    index: React.PropTypes.object.isRequired,
    indexRange: React.PropTypes.object.isRequired,
  },
  render() {
    const { index, indexRange } = this.props;
    return (
      <div className="index-info">
        Range re-calculated {moment(indexRange.calculated_at).fromNow()} in {indexRange.took_ms}ms.{' '}

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
      </div>
    );
  },
});

export default IndexDetails;
