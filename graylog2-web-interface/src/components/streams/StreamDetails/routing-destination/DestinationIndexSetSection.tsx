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
  /* eslint-disable no-constant-condition */
  const title = true ? 'Enabled' : 'Disabled'; // TODO use api to check if enabled
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
      title="Index Set"
      collapsible
      defaultClosed
      headerLeftSection={
        <>
          <DestinationSwitch
            aria-label="Toggle index set"
            name="toggle-indexset"
            checked
            label={title}
            disabled
            onChange={() => {}}
          />
          <SectionCountLabel>FILTERS {streamOutputFilters?.pagination?.total || 0}</SectionCountLabel>
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
        Messages routed to the <b>Search Cluster</b> will be searchable and count towards the {productName} License
        usage. These messages will be stored in the defined Index Set until the retention policy criteria is met. Note:
        Messages not routed to the <b>Search Cluster</b> will not be searchable.
      </Alert>
      <Table>
        <thead>
          <tr>
            <td>Name</td>
            <td>Total size</td>
            <td>Oldest Message (date)</td>
            <td>Archiving</td>
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
