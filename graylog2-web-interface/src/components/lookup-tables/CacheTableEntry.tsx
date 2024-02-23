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

import useHistory from 'routing/useHistory';
import { Link } from 'components/common/router';
import { Spinner } from 'components/common';
import { Button } from 'components/bootstrap';
import Routes from 'routing/Routes';
import { MetricsMapper, MetricContainer, CounterRate } from 'components/metrics';
import NumberUtils from 'util/NumberUtils';
import { LookupTableCachesActions } from 'stores/lookup-tables/LookupTableCachesStore';
import type { LookupTableCache } from 'logic/lookup-tables/types';
import useScopePermissions from 'hooks/useScopePermissions';
import ButtonToolbar from 'components/bootstrap/ButtonToolbar';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

type Props = {
  cache: LookupTableCache,
};

const Actions = styled(ButtonToolbar)`
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: flex-start;
`;

const CacheTableEntry = ({ cache }: Props) => {
  const history = useHistory();
  const { loadingScopePermissions, scopePermissions } = useScopePermissions(cache);
  const sendTelemetry = useSendTelemetry();

  const countMap = {
    requests: `org.graylog2.lookup.caches.${cache.id}.requests`,
    hits: `org.graylog2.lookup.caches.${cache.id}.hits`,
    misses: `org.graylog2.lookup.caches.${cache.id}.misses`,
  };

  const entriesMap = {
    count: `org.graylog2.lookup.caches.${cache.id}.entries`,
  };

  const computeEntriesMetrics = (metrics: any) => {
    const total = Object.keys(metrics).reduce((acc: number, nodeId: string) => (
      acc + (Number.isNaN(metrics[nodeId].count.metric.value) ? 0 : metrics[nodeId].count.metric.value)
    ), 0);

    if (total < 0) return 'N/A';

    return NumberUtils.formatNumber(total);
  };

  const computeCountMetrics = (metrics: any) => {
    const totalHits = Object.keys(metrics).reduce((acc: number, nodeId: string) => (
      acc + (Number.isNaN(metrics[nodeId].hits.metric.rate.total) ? 0 : metrics[nodeId].hits.metric.rate.total)
    ), 0);

    const totalMisses = Object.keys(metrics).reduce((acc: number, nodeId: string) => (
      acc + (Number.isNaN(metrics[nodeId].misses.metric.rate.total) ? 0 : metrics[nodeId].misses.metric.rate.total)
    ), 0);

    const total = totalHits + totalMisses;
    if (total < 1) return 'N/A';
    const hitRate = (totalHits * 100.0) / total;

    return `${NumberUtils.formatNumber(hitRate)}%`;
  };

  const handleEdit = React.useCallback(() => {
    history.push(Routes.SYSTEM.LOOKUPTABLES.CACHES.edit(cache.name));
  }, [history, cache.name]);

  const handleDelete = React.useCallback(() => {
    // eslint-disable-next-line no-alert
    const shouldDelete = window.confirm(`Are you sure you want to delete cache "${cache.title}"?`);

    if (shouldDelete) {
      LookupTableCachesActions.delete(cache.id).then(() => {
        sendTelemetry(TELEMETRY_EVENT_TYPE.LUT.CACHE_DELETED, {
          app_pathname: 'lut',
          app_section: 'lut_cache',
        });

        LookupTableCachesActions.reloadPage();
      });
    }
  }, [cache.title, cache.id, sendTelemetry]);

  return (
    <tbody>
      <tr>
        <td>
          <Link to={Routes.SYSTEM.LOOKUPTABLES.CACHES.show(cache.name)}>{cache.title}</Link>
        </td>
        <td>{cache.description}</td>
        <td>{cache.name}</td>
        <td>
          <MetricsMapper map={entriesMap} computeValue={computeEntriesMetrics} />
        </td>
        <td>
          <MetricsMapper map={countMap} computeValue={computeCountMetrics} />
        </td>
        <td>
          <MetricContainer name={`org.graylog2.lookup.caches.${cache.id}.requests`}>
            <CounterRate suffix="lookups/s" />
          </MetricContainer>
        </td>
        <td>
          {loadingScopePermissions ? <Spinner /> : scopePermissions.is_mutable && (
            <Actions>
              <Button bsSize="xsmall"
                      onClick={handleEdit}
                      role="button"
                      name="edit_square">
                Edit
              </Button>
              <Button bsSize="xsmall"
                      bsStyle="danger"
                      onClick={handleDelete}
                      role="button"
                      name="delete">
                Delete
              </Button>
            </Actions>
          )}
        </td>
      </tr>
    </tbody>
  );
};

export default CacheTableEntry;
