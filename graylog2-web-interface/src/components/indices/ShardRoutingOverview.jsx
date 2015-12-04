import React from 'react';

import { ShardRouting } from 'components/indices';

const ShardRoutingOverview = React.createClass({
  propTypes: {
    routing: React.PropTypes.array.isRequired,
  },
  render() {
    return (
      <div className="shard-routing">
        <h3>Shard routing</h3>

        <ul className="shards">
          {this.props.routing.map((route) => <ShardRouting key={'shard-route-' + route.id} route={route}/>)}
        </ul>

        <br style={{clear: 'both'}} />

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
