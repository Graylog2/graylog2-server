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
import { useMemo, useCallback } from 'react';
import styled, { css } from 'styled-components';

import { Spinner } from 'components/common';
import PaginatedEntityTable from 'components/common/PaginatedEntityTable';
import type { SearchParams } from 'stores/PaginationTypes';

import { useCollectorStats, fetchPaginatedSources, sourcesKeyFn } from '../hooks';
import StatCard from '../common/StatCard';
import sourceColumnRenderers from '../sources/ColumnRenderers';
import { DEFAULT_LAYOUT as SOURCES_LAYOUT } from '../sources/Constants';
import type { Source } from '../types';

const StatsRow = styled.div(
  ({ theme }) => css`
    display: flex;
    margin-bottom: ${theme.spacings.lg};
    gap: ${theme.spacings.md};
    flex-wrap: wrap;
  `,
);

const Section = styled.div(
  ({ theme }) => css`
    margin-top: ${theme.spacings.lg};
  `,
);

const CollectorsOverview = () => {
  const { data: stats, isLoading: statsLoading } = useCollectorStats();

  const sourceRenderers = useMemo(() => sourceColumnRenderers(), []);

  const fetchSources = useCallback(
    (searchParams: SearchParams) => fetchPaginatedSources(searchParams),
    [],
  );

  if (statsLoading) {
    return <Spinner />;
  }

  return (
    <div>
      <StatsRow>
        <StatCard value={stats?.total_instances || 0} label="Instances" />
        <StatCard value={stats?.online_instances || 0} label="Online" variant="success" />
        <StatCard value={stats?.offline_instances || 0} label="Offline" variant="warning" />
        <StatCard value={stats?.total_fleets || 0} label="Fleets" />
        <StatCard value={stats?.total_sources || 0} label="Sources" />
      </StatsRow>

      <Section>
        <h4 style={{ marginBottom: '1rem' }}>Active Sources</h4>
        <PaginatedEntityTable<Source>
          humanName="sources"
          tableLayout={SOURCES_LAYOUT}
          fetchEntities={fetchSources}
          keyFn={sourcesKeyFn}
          entityAttributesAreCamelCase={false}
          columnRenderers={sourceRenderers}
          entityActions={() => null}
        />
      </Section>
    </div>
  );
};

export default CollectorsOverview;
