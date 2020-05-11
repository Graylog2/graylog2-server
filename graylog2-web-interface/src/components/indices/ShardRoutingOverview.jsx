import PropTypes from 'prop-types';
import React from 'react';
import styled from 'styled-components';
import naturalSort from 'javascript-natural-sort';

import { ShardRouting } from 'components/indices';

const ShardRoutingWrap = styled.div(({ theme }) => `
  .shards {
    .shard {
      padding: 10px;
      margin: 5px;
      width: 50px;
      float: left;
      text-align: center;
    }

    .shard-started {
      background-color: ${theme.util.colorLevel(theme.color.variant.light.success, -2)};
    }

    .shard-relocating {
      background-color: ${theme.util.colorLevel(theme.color.variant.light.primary, -2)};
    }

    .shard-initializing {
      background-color: ${theme.util.colorLevel(theme.color.variant.light.warning, -5)};
    }

    .shard-unassigned {
      background-color: ${theme.util.colorLevel(theme.color.variant.light.default, -2)};
    }

    .shard-primary .id {
      font-weight: bold;
      margin-bottom: 3px;
      border-bottom: 1px solid ${theme.color.gray[10]};
    }
  }

  .description {
    font-size: 11px;
    margin-top: 2px;
    margin-left: 6px;
  }
`);

const ShardRoutingOverview = ({ indexName, routing }) => {
  return (
    <ShardRoutingWrap>
      <h3>Shard routing</h3>

      <ul className="shards">
        {routing
          .sort((shard1, shard2) => naturalSort(shard1.id, shard2.id))
          .map((route) => <ShardRouting key={`${indexName}-shard-route-${route.node_id}-${route.id}`} route={route} />)}
      </ul>
      <br style={{ clear: 'both' }} />

      <div className="description">
        Bold shards are primaries, others are replicas. Replicas are elected to primaries automatically
        when primaries leave the cluster. Size and document counts only reflect primary shards and no
        possible replica duplication.
      </div>
    </ShardRoutingWrap>
  );
};

ShardRoutingOverview.propTypes = {
  routing: PropTypes.array.isRequired,
  indexName: PropTypes.string.isRequired,
};

export default ShardRoutingOverview;
