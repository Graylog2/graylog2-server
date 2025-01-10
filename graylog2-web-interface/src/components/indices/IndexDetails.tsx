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
import React, { useCallback, useEffect, useMemo } from 'react';

import HideOnCloud from 'util/conditional/HideOnCloud';
import NumberUtils from 'util/NumberUtils';
import { Col, Row, Button } from 'components/bootstrap';
import { Spinner } from 'components/common';
import { IndexRangeSummary, ShardMeter, ShardRoutingOverview } from 'components/indices';
import type { IndexInfo } from 'stores/indices/IndicesStore';
import type { IndexRange } from 'stores/indices/IndexRangesStore';
import { IndexRangesActions } from 'stores/indices/IndexRangesStore';
import { IndicesActions } from 'stores/indices/IndicesStore';

type Props = {
  index: IndexInfo,
  indexName: string,
  indexRange: IndexRange,
  indexSetId: string,
  isDeflector: boolean,
};

const IndexDetails = ({ index, indexName, indexRange, indexSetId, isDeflector }: Props) => {
  useEffect(() => {
    IndicesActions.subscribe(indexName);

    return () => {
      IndicesActions.unsubscribe(indexName);
    };
  }, [indexName]);

  const _onRecalculateIndex = useCallback(() => {
    // eslint-disable-next-line no-alert
    if (window.confirm(`Really recalculate the index ranges for index ${indexName}?`)) {
      IndexRangesActions.recalculateIndex(indexName).then(() => {
        IndicesActions.list(indexSetId);
      });
    }
  }, [indexName, indexSetId]);

  const _onDeleteIndex = useCallback(() => {
    // eslint-disable-next-line no-alert
    if (window.confirm(`Really delete index ${indexName}?`)) {
      IndicesActions.delete(indexName).then(() => {
        IndicesActions.list(indexSetId);
      });
    }
  }, [indexName, indexSetId]);

  const actionButtons = useMemo(() => {
    if (isDeflector) {
      return (
        <span>
          <Button bsStyle="danger" bsSize="xs" disabled>Active write index cannot be deleted</Button>
        </span>
      );
    }

    return (
      <span>
        <Button bsStyle="warning" bsSize="xs" onClick={_onRecalculateIndex}>Recalculate index ranges</Button>{' '}
        <Button bsStyle="danger" bsSize="xs" onClick={_onDeleteIndex}>Delete index</Button>
      </span>
    );
  }, [isDeflector, _onDeleteIndex, _onRecalculateIndex]);

  if (!index || !index.all_shards) {
    return <Spinner />;
  }

  return (
    <div className="index-info">
      <IndexRangeSummary indexRange={indexRange} />{' '}

      <HideOnCloud>
        {NumberUtils.formatNumber(index.all_shards.segments)} segments,{' '}
        {NumberUtils.formatNumber(index.all_shards.open_search_contexts)} open search contexts,{' '}
        {NumberUtils.formatNumber(index.all_shards.documents.deleted)} deleted messages
        <Row style={{ marginBottom: '10' }}>
          <Col md={4} className="shard-meters">
            <ShardMeter title="Primary shard operations" shardMeter={index.primary_shards} />
          </Col>
          <Col md={4} className="shard-meters">
            <ShardMeter title="Total shard operations" shardMeter={index.all_shards} />
          </Col>
        </Row>
        <ShardRoutingOverview routing={index.routing} indexName={indexName} />
      </HideOnCloud>
      <hr style={{ marginBottom: '5', marginTop: '10' }} />

      {actionButtons}
    </div>
  );
};

export default IndexDetails;
