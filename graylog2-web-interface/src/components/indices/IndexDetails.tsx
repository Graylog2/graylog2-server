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
import React, { useCallback, useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { useQueryClient } from '@tanstack/react-query';

import { SystemIndexRanges } from '@graylog/server-api';

import HideOnCloud from 'util/conditional/HideOnCloud';
import { defaultOnError } from 'util/conditional/onError';
import NumberUtils from 'util/NumberUtils';
import { Col, Row, Button } from 'components/bootstrap';
import { Spinner } from 'components/common';
import IndexRangeSummary from 'components/indices/IndexRangeSummary';
import ShardMeter from 'components/indices/ShardMeter';
import ShardRoutingOverview from 'components/indices/ShardRoutingOverview';
import type { IndexInfo } from 'hooks/useIndices';
import type { IndexRange } from 'hooks/useIndexerOverview';
import { INDICES_QUERY_KEY, deleteIndex } from 'hooks/useIndices';

type Props = {
  index: IndexInfo;
  indexName: string;
  indexRange?: IndexRange;
  indexSetId?: string;
  isDeflector: boolean;
};

const IndexDetails = ({ index, indexName, indexRange = undefined, indexSetId = undefined, isDeflector }: Props) => {
  const { t } = useTranslation('indexsets');
  const queryClient = useQueryClient();

  const _invalidate = useCallback(() => queryClient.invalidateQueries({ queryKey: INDICES_QUERY_KEY }), [queryClient]);

  const _onRecalculateIndex = useCallback(() => {
    // eslint-disable-next-line no-alert
    if (window.confirm(t('indices.details.confirm_recalculate', { indexName }))) {
      defaultOnError(
        SystemIndexRanges.rebuildIndex(indexName),
        t('indices.details.recalculate_failed', { indexName }),
        t('indices.details.recalculate_failed_title', { indexName }),
      ).then(() => {
        if (indexSetId) {
          _invalidate();
        }
      });
    }
  }, [indexName, indexSetId, _invalidate, t]);

  const _onDeleteIndex = useCallback(() => {
    // eslint-disable-next-line no-alert
    if (window.confirm(t('indices.details.confirm_delete', { indexName }))) {
      deleteIndex(indexName).then(() => {
        if (indexSetId) {
          _invalidate();
        }
      });
    }
  }, [indexName, indexSetId, _invalidate, t]);

  const actionButtons = useMemo(() => {
    if (isDeflector) {
      return (
        <span>
          <Button bsStyle="danger" bsSize="xs" disabled>
            {t('indices.details.active_write_index')}
          </Button>
        </span>
      );
    }

    return (
      <span>
        <Button bsStyle="warning" bsSize="xs" onClick={_onRecalculateIndex}>
          {t('indices.details.recalculate')}
        </Button>{' '}
        <Button bsStyle="danger" bsSize="xs" onClick={_onDeleteIndex}>
          {t('indices.details.delete')}
        </Button>
      </span>
    );
  }, [isDeflector, _onDeleteIndex, _onRecalculateIndex, t]);

  if (!index || !index.all_shards) {
    return <Spinner />;
  }

  return (
    <div className="index-info">
      <IndexRangeSummary indexRange={indexRange} />{' '}
      <HideOnCloud>
        {t('indices.details.stats', {
          segments: NumberUtils.formatNumber(index.all_shards.segments),
          contexts: NumberUtils.formatNumber(index.all_shards.open_search_contexts),
          deleted: NumberUtils.formatNumber(index.all_shards.documents.deleted),
        })}
        <Row style={{ marginBottom: '10' }}>
          <Col md={4} className="shard-meters">
            <ShardMeter title={t('indices.details.primary_shard_operations')} shardMeter={index.primary_shards} />
          </Col>
          <Col md={4} className="shard-meters">
            <ShardMeter title={t('indices.details.total_shard_operations')} shardMeter={index.all_shards} />
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
