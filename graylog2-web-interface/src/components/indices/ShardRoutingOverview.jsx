import PropTypes from 'prop-types';
import React from 'react';
import styled from 'styled-components';

import { ShardRouting } from 'components/indices';
import naturalSort from 'javascript-natural-sort';

const ShardRoutingWrap = styled.div`
  .shards .shard {
    padding: 10px;
    margin: 5px;
    width: 50px;
    float: left;
    text-align: center;
  }

  .shards .shard-started {
    background-color: #dff0d8;
  }

  .shards .shard-relocating {
    background-color: #de9df4;
  }

  .shards .shard-initializing {
    background-color: #f4ddbc;
  }

  .shards .shard-unassigned {
    background-color: #c3c3c3;
  }

  .shards .shard-primary .id {
    font-weight: bold;
    margin-bottom: 3px;
    border-bottom: 1px solid #000;
  }

  .description {
    font-size: 11px;
    margin-top: 2px;
    margin-left: 6px;
  }
`;

class ShardRoutingOverview extends React.Component {
  static propTypes = {
    routing: PropTypes.array.isRequired,
    indexName: PropTypes.string.isRequired,
  };

  render() {
    const { indexName, routing } = this.props;
    return (
      <ShardRoutingWrap>
        <h3>Shard routing</h3>

        <ul className="shards">
          {routing
            .sort((shard1, shard2) => naturalSort(shard1.id, shard2.id))
            .map(route => <ShardRouting key={`${indexName}-shard-route-${route.node_id}-${route.id}`} route={route} />)}
        </ul>

        <br style={{ clear: 'both' }} />

        <div className="description">
          Bold shards are primaries, others are replicas. Replicas are elected to primaries automatically
          when primaries leave the cluster. Size and document counts only reflect primary shards and no
          possible replica duplication.
        </div>
      </ShardRoutingWrap>
    );
  }
}

export default ShardRoutingOverview;
