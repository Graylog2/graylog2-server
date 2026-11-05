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
import { useState } from 'react';
import { Trans, useTranslation } from 'react-i18next';

import { ARCHIVE_RETENTION_STRATEGY } from 'hooks/useIndices';
import { Section, Spinner, Link } from 'components/common';
import { Table, Alert } from 'components/bootstrap';
import Routes from 'routing/Routes';
import useIndexSetsList from 'components/indices/hooks/useIndexSetsList';
import type { Stream } from 'logic/streams/types';
import NumberUtils from 'util/NumberUtils';
import useStreamOutputFilters from 'components/streams/hooks/useStreamOutputFilters';
import useExcludedArchiveStreams from 'components/streams/hooks/useExcludedArchiveStreams';
import IndexSetArchivingCell from 'components/streams/StreamDetails/routing-destination/IndexSetArchivingCell';
import IndexSetUpdateForm from 'components/streams/StreamDetails/routing-destination/IndexSetUpdateForm';
import IndexSetFilters from 'components/streams/StreamDetails/routing-destination/IndexSetFilters';
import DestinationSwitch from 'components/streams/StreamDetails/routing-destination/DestinationSwitch';
import SectionCountLabel from 'components/streams/StreamDetails/SectionCountLabel';
import useIndexSetStats from 'hooks/useIndexSetStats';
import { DEFAULT_PAGINATION } from 'stores/PaginationTypes';
import useIndexerOverview from 'hooks/useIndexerOverview';
import useSingleIndexSet from 'components/indices/hooks/useSingleIndexSet';
import useProductName from 'brand-customization/useProductName';

import IndexSetOldestMessageCell from './IndexSetOldestMessageCell';

type Props = {
  stream: Stream;
};

const DestinationIndexSetSection = ({ stream }: Props) => {
  const { t } = useTranslation('streams');
  const productName = useProductName();
  const [pagination, setPagination] = useState(DEFAULT_PAGINATION);
  const { data: indexSet, isInitialLoading: isLoadingIndexSet } = useSingleIndexSet(stream.index_set_id);
  const excludedStreams = useExcludedArchiveStreams();
  const indexSetArchivingEnabled =
    indexSet?.retention_strategy_class === ARCHIVE_RETENTION_STRATEGY ||
    indexSet?.data_tiering?.archive_before_deletion;
  // A stream is only archived when its index set archives AND the stream is not excluded from archiving.
  const archivingEnabled = Boolean(indexSetArchivingEnabled) && !excludedStreams.includes(stream.id);
  const {
    data: { indexSets },
  } = useIndexSetsList(false);
  const { data: streamOutputFilters, isLoading: isLoadingStreamOutputFilters } = useStreamOutputFilters(
    stream.id,
    'indexer',
    pagination,
  );
  const { data: indexerOverview, isSuccess: isLoadingIndexerOverviewSuccess } = useIndexerOverview(stream.index_set_id);
  const title = t('routing_destination.index_set.enabled'); // TODO use api to check if enabled
  const { data: indexSetStats, isSuccess: isStatsLoaded } = useIndexSetStats(stream.index_set_id);

  if (isLoadingStreamOutputFilters || isLoadingIndexSet) {
    <Spinner />;
  }

  const onPaginationChange = (newPage: number, newPerPage: number) =>
    setPagination({
      ...pagination,
      page: newPage,
      perPage: newPerPage,
    });

  return (
    <Section
      title={t('routing_destination.index_set.title')}
      collapsible
      defaultClosed
      headerLeftSection={
        <>
          <DestinationSwitch
            aria-label={t('routing_destination.index_set.toggle_aria')}
            name="toggle-indexset"
            checked
            label={title}
            disabled
            onChange={() => {}}
          />
          <SectionCountLabel>
            {t('routing_destination.index_set.filters', { count: streamOutputFilters?.pagination?.total || 0 })}
          </SectionCountLabel>
        </>
      }
      actions={
        <IndexSetUpdateForm
          initialValues={{ index_set_id: stream.index_set_id }}
          indexSets={indexSets}
          stream={stream}
        />
      }>
      <Alert bsStyle="default">
        <Trans
          t={t}
          i18nKey="routing_destination.index_set.info"
          values={{ productName }}
          components={{ boldText: <b /> }}
        />
      </Alert>
      <Table>
        <thead>
          <tr>
            <td>{t('routing_destination.index_set.col_name')}</td>
            <td>{t('routing_destination.index_set.col_size')}</td>
            <td>{t('routing_destination.index_set.col_oldest')}</td>
            <td>{t('routing_destination.index_set.col_archiving')}</td>
          </tr>
        </thead>
        <tbody>
          {indexSet && (
            <tr>
              <td>
                <Link to={Routes.SYSTEM.INDEX_SETS.SHOW(indexSet?.id)}>{indexSet?.title}</Link>
              </td>
              <td>{isStatsLoaded && indexSetStats?.size ? NumberUtils.formatBytes(indexSetStats.size) : 0}</td>
              <td>
                {isLoadingIndexerOverviewSuccess && (
                  <IndexSetOldestMessageCell index={indexerOverview?.indices?.pop()} />
                )}
              </td>
              <td>
                <IndexSetArchivingCell isArchivingEnabled={archivingEnabled} streamId={stream.id} />
              </td>
            </tr>
          )}
        </tbody>
      </Table>
      {streamOutputFilters && (
        <IndexSetFilters
          streamId={stream.id}
          paginatedFilters={streamOutputFilters}
          onPaginationChange={onPaginationChange}
        />
      )}
    </Section>
  );
};

export default DestinationIndexSetSection;
