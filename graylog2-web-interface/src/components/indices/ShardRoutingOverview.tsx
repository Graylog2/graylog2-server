import React from 'react';
import styled, { css } from 'styled-components';

import { defaultCompare as naturalSort } from 'logic/DefaultCompare';
import ShardRouting from 'components/indices/ShardRouting';

const ShardRoutingWrap = styled.div(({ theme }) => css`
  .shards {
    .shard {
      padding: 10px;
      margin: 5px;
      width: 50px;
      float: left;
      text-align: center;
    }

    .shard-started {
      background-color: ${theme.utils.colorLevel(theme.colors.variant.light.success, -2)};
    }

    .shard-relocating {
      background-color: ${theme.utils.colorLevel(theme.colors.variant.light.primary, -2)};
    }

    .shard-initializing {
      background-color: ${theme.utils.colorLevel(theme.colors.variant.light.warning, -5)};
    }

    .shard-unassigned {
      background-color: ${theme.utils.colorLevel(theme.colors.variant.light.default, -2)};
    }

    .shard-primary .id {
      font-weight: bold;
      margin-bottom: 3px;
      border-bottom: 1px solid ${theme.colors.gray[10]};
    }
  }

  .description {
    font-size: ${theme.fonts.size.small};
    margin-top: 2px;
    margin-left: 6px;
  }
`);

type ShardRoutingOverviewProps = {
  routing: any[];
  indexName: string;
};

const ShardRoutingOverview = ({
  indexName,
  routing
}: ShardRoutingOverviewProps) => (
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

export default ShardRoutingOverview;