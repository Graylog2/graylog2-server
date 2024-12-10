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
import styled, { css } from 'styled-components';

import { ARCHIVE_RETENTION_STRATEGY } from 'stores/indices/IndicesStore';
import { Icon, Section, Spinner } from 'components/common';
import { IndexSetsStore } from 'stores/indices/IndexSetsStore';
import { Table, Button, Alert } from 'components/bootstrap';
import { LinkContainer } from 'components/common/router';
import Routes from 'routing/Routes';
import { useStore } from 'stores/connect';
import type { Stream } from 'stores/streams/StreamsStore';
import NumberUtils from 'util/NumberUtils';
import useStreamOutputFilters from 'components/streams/hooks/useStreamOutputFilters';
import IndexSetArchivingCell from 'components/streams/StreamDetails/routing-destination/IndexSetArchivingCell';
import IndexSetUpdateForm from 'components/streams/StreamDetails/routing-destination/IndexSetUpdateForm';
import IndexSetFilters from 'components/streams/StreamDetails/routing-destination/IndexSetFilters';
import DestinationSwitch from 'components/streams/StreamDetails/routing-destination/DestinationSwitch';
import SectionCountLabel from 'components/streams/StreamDetails/SectionCountLabel';
import useIndexSetStats from 'hooks/useIndexSetStats';
import { DEFAULT_PAGINATION } from 'stores/PaginationTypes';
import useIndexerOverview from 'hooks/useIndexerOverview';
import useSingleIndexSet from 'components/indices/hooks/useSingleIndexSet';

import IndexSetOldestMessageCell from './IndexSetOldestMessageCell';

type Props = {
  stream: Stream,
};

const ActionButtonsWrap = styled.span(() => css`
  float: right;
`);

const DestinationIndexSetSection = ({ stream }: Props) => {
  const [pagination, setPagination] = useState(DEFAULT_PAGINATION);
  const { data: indexSet, isInitialLoading: isLoadingIndexSet } = useSingleIndexSet(stream.index_set_id);
  const archivingEnabled = indexSet?.retention_strategy_class === ARCHIVE_RETENTION_STRATEGY || indexSet?.data_tiering?.archive_before_deletion;
  const { indexSets } = useStore(IndexSetsStore);
  const { data: streamOutputFilters, isLoading: isLoadingStreamOutputFilters } = useStreamOutputFilters(stream.id, 'indexer', pagination);
  const { data: indexerOverview, isSuccess: isLoadingIndexerOverviewSuccess } = useIndexerOverview(stream.index_set_id);
  /* eslint-disable no-constant-condition */
  const title = true ? 'Enabled' : 'Disabled'; // TODO use api to check if enabled
  const { data: indexSetStats, isSuccess: isStatsLoaded } = useIndexSetStats(stream.index_set_id);

  if (isLoadingStreamOutputFilters || isLoadingIndexSet) {
    <Spinner />;
  }

  const onPaginationChange = (newPage: number, newPerPage: number) => setPagination({
    ...pagination,
    page: newPage,
    perPage: newPerPage,
  });

  return (
    <Section title="Index Set"
             collapsible
             defaultClosed
             headerLeftSection={(
               <>
                 <DestinationSwitch aria-label="Toggle index set"
                                    name="toggle-indexset"
                                    checked
                                    label={title}
                                    disabled
                                    onChange={() => {}} />
                 <SectionCountLabel>FILTERS {streamOutputFilters?.pagination?.total || 0}</SectionCountLabel>
               </>
             )}
             actions={(
               <IndexSetUpdateForm initialValues={{ index_set_id: stream.index_set_id }}
                                   indexSets={indexSets}
                                   stream={stream} />
            )}>
      <Alert bsStyle="default">
        Messages routed to the <b>Search Cluster</b> will be searchable in Graylog and count towards Graylog License usage.<br />
        These messages will be stored in the defined Index Set until the retention policy criteria is met.<br />
        Note: Messages not routed to the <b>Search Cluster</b> will not be searchable in Graylog.
      </Alert>
      <Table>
        <thead>
          <tr>
            <td>Name</td>
            <td>Total size</td>
            <td>Oldest Message (date)</td>
            <td colSpan={2}>Archiving</td>
          </tr>
        </thead>
        <tbody>
          {indexSet && (
            <tr>
              <td>{indexSet?.title}</td>
              <td>{(isStatsLoaded && indexSetStats?.size) ? NumberUtils.formatBytes(indexSetStats.size) : 0}</td>
              <td>{isLoadingIndexerOverviewSuccess && <IndexSetOldestMessageCell index={indexerOverview?.indices?.pop()} />}</td>
              <td>
                <IndexSetArchivingCell isArchivingEnabled={archivingEnabled} streamId={stream.id} />
              </td>
              <td>
                <ActionButtonsWrap>
                  <LinkContainer to={Routes.SYSTEM.INDEX_SETS.SHOW(indexSet?.id)}>
                    <Button bsStyle="default"
                            bsSize="xsmall"
                            onClick={() => {}}
                            title="View index set">
                      <Icon name="pageview" type="regular" />
                    </Button>
                  </LinkContainer>
                </ActionButtonsWrap>
              </td>
            </tr>
          )}
        </tbody>
      </Table>
      {streamOutputFilters && (<IndexSetFilters streamId={stream.id} paginatedFilters={streamOutputFilters} onPaginationChange={onPaginationChange} />)}
    </Section>
  );
};

export default DestinationIndexSetSection;
