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
import * as React from 'react';
import styled from 'styled-components';

import NumberUtils from 'util/NumberUtils';
import { MetricsMapper, MetricContainer, CounterRate } from 'components/metrics';
import { useModalContext } from 'components/lookup-tables/LUTModals/ModalContext';
import Cache from 'components/lookup-tables/Cache';
import type { ColumnRenderers } from 'components/common/EntityDataTable';

import type { CacheEntity } from './types';

const TitleRow = styled.div`
  display: flex;
  flex-direction: row;
  align-items: flex-end;
`;

const Title = styled.div`
  color: ${({ theme }) => theme.colors.link.default};
  text-overflow: ellipsis;
  overflow: hidden;
  white-space: nowrap;
  cursor: pointer;

  &:hover {
    color: ${({ theme }) => theme.colors.link.hover};
    text-decoration: underline;
  }
`;

const TitleCol = ({ cache, children }: { cache: CacheEntity; children: string }) => {
  const { setModal, setTitle, setEntity } = useModalContext();

  const onClick = React.useCallback(() => {
    setModal('CACHE');
    setTitle(cache.name);
    setEntity(<Cache cache={cache} />);
  }, [cache, setModal, setTitle, setEntity]);

  return (
    <TitleRow>
      <Title onClick={onClick}>{children}</Title>
    </TitleRow>
  );
};

const EntriesCol = ({ cache }: { cache: CacheEntity }) => {
  const entriesMap = {
    count: `org.graylog2.lookup.caches.${cache.id}.entries`,
  };

  const computeEntriesMetrics = (metrics: any) => {
    const total = Object.keys(metrics).reduce(
      (acc: number, nodeId: string) =>
        acc + (Number.isNaN(metrics[nodeId].count.metric.value) ? 0 : metrics[nodeId].count.metric.value),
      0,
    );

    if (total < 1) return 'N/A';

    return NumberUtils.formatNumber(total);
  };

  return <MetricsMapper map={entriesMap} computeValue={computeEntriesMetrics} />;
};

const HitRateCol = ({ cache }: { cache: CacheEntity }) => {
  const countMap = {
    requests: `org.graylog2.lookup.caches.${cache.id}.requests`,
    hits: `org.graylog2.lookup.caches.${cache.id}.hits`,
    misses: `org.graylog2.lookup.caches.${cache.id}.misses`,
  };

  const computeCountMetrics = (metrics: any) => {
    const totalHits = Object.keys(metrics).reduce(
      (acc: number, nodeId: string) =>
        acc + (Number.isNaN(metrics[nodeId].hits.metric.rate.total) ? 0 : metrics[nodeId].hits.metric.rate.total),
      0,
    );

    const totalMisses = Object.keys(metrics).reduce(
      (acc: number, nodeId: string) =>
        acc + (Number.isNaN(metrics[nodeId].misses.metric.rate.total) ? 0 : metrics[nodeId].misses.metric.rate.total),
      0,
    );

    const total = totalHits + totalMisses;
    if (total < 1) return 'N/A';
    const hitRate = (totalHits * 100.0) / total;

    return `${NumberUtils.formatNumber(hitRate)}%`;
  };

  return <MetricsMapper map={countMap} computeValue={computeCountMetrics} />;
};

const ThroughputCol = ({ cache }: { cache: CacheEntity }) => (
  <MetricContainer name={`org.graylog2.lookup.caches.${cache.id}.requests`}>
    <CounterRate suffix="lookups/s" />
  </MetricContainer>
);

const columnRenderers: ColumnRenderers<CacheEntity> = {
  attributes: {
    title: {
      width: 0.2,
      renderCell: (title: string, cache: CacheEntity) => <TitleCol cache={cache}>{title}</TitleCol>,
    },
    description: {
      width: 0.2,
      renderCell: (description: string) => <span>{description}</span>,
    },
    name: {
      width: 0.2,
      renderCell: (name: string) => <span>{name}</span>,
    },
    entries: {
      staticWidth: 150,
      renderCell: (_arg: unknown, cache: CacheEntity) => <EntriesCol cache={cache} />,
    },
    hit_rate: {
      staticWidth: 150,
      renderCell: (_arg: unknown, cache: CacheEntity) => <HitRateCol cache={cache} />,
    },
    throughput: {
      staticWidth: 150,
      renderCell: (_arg: unknown, cache: CacheEntity) => <ThroughputCol cache={cache} />,
    },
  },
};

export default columnRenderers;
