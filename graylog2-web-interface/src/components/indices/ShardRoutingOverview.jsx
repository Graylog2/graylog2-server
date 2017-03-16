import React from 'react';

import { ShardRouting } from 'components/indices';
import naturalSort from 'javascript-natural-sort';

const ShardRoutingOverview = React.createClass({
  propTypes: {
    routing: React.PropTypes.array.isRequired,
    indexName: React.PropTypes.string.isRequired,
  },
  render() {
    const { indexName, routing } = this.props;
    return (
      <div className="shard-routing">
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
      </div>
    );
  },
});

export default ShardRoutingOverview;
