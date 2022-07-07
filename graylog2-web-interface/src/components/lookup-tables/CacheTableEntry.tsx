import * as React from 'react';
import { useHistory } from 'react-router-dom';
import styled from 'styled-components';

import { Link } from 'components/common/router';
import { Button } from 'components/bootstrap';
import Routes from 'routing/Routes';
import { MetricsMapper, MetricContainer, CounterRate } from 'components/metrics';
import NumberUtils from 'util/NumberUtils';
import { LookupTableCachesActions } from 'stores/lookup-tables/LookupTableCachesStore';
import type { LookupTableCache } from 'logic/lookup-tables/types';
import useGetPermissionsByScope from 'logic/lookup-tables/useScopePermissions';

type Props = {
  cache: LookupTableCache,
};

const Actions = styled.div`
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: flex-start;
`;

const CacheTableEntry = ({ cache }: Props) => {
  const history = useHistory();
  const { getPermissionsByScope } = useGetPermissionsByScope();

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

  const showAction = (inCache: LookupTableCache, action: string): boolean => {
    // TODO: Update this method to check for the metadata
    const permissions = getPermissionsByScope(inCache._metadata?.scope);

    return permissions[action];
  };

  const handleEdit = (cacheName: string) => (_event: React.SyntheticEvent) => {
    history.push(Routes.SYSTEM.LOOKUPTABLES.CACHES.edit(cacheName));
  };

  const handleDelete = (inCache: LookupTableCache) => {
    // eslint-disable-next-line no-alert
    const shouldDelete = window.confirm(`Are you sure you want to delete cache "${inCache.title}"?`);

    if (shouldDelete) {
      LookupTableCachesActions.delete(inCache.id).then(() => LookupTableCachesActions.reloadPage());
    }
  };

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
          <Actions>
            {showAction(cache, 'edit') && (
              <Button bsSize="xsmall" bsStyle="info" onClick={handleEdit(cache.name)} role="edit-button">
                Edit
              </Button>
            )}
            {showAction(cache, 'delete') && (
              <Button style={{ marginLeft: '6px' }}
                      bsSize="xsmall"
                      bsStyle="primary"
                      onClick={handleDelete}
                      role="delete-button">
                Delete
              </Button>
            )}
          </Actions>
        </td>
      </tr>
    </tbody>
  );
};

export default CacheTableEntry;
