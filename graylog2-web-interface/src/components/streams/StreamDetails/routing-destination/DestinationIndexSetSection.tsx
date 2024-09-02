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
import { Icon, Section, Spinner, Switch, Timestamp } from 'components/common';
import { IndexSetsStore, type IndexSet } from 'stores/indices/IndexSetsStore';
import { Table, Button, Alert } from 'components/bootstrap';
import { LinkContainer } from 'components/common/router';
import Routes from 'routing/Routes';
import { useStore } from 'stores/connect';
import type { Stream } from 'stores/streams/StreamsStore';
import useStreamOutputFilters from 'components/streams/hooks/useStreamOutputFilters';
import IndexSetArchivingCell from 'components/streams/StreamDetails/routing-destination/IndexSetArchivingCell';
import IndexSetUpdateForm from 'components/streams/StreamDetails/routing-destination/IndexSetUpdateForm';
import IndexSetFilters from 'components/streams/StreamDetails/routing-destination/IndexSetFilters';
import SectionCountLabel from 'components/streams/StreamDetails/SectionCountLabel';
import useIndexSetStats from 'hooks/useIndexSetStats';
import NumberUtils from 'util/NumberUtils';
import { DEFAULT_PAGINATION } from 'stores/PaginationTypes';

type Props = {
  indexSet: IndexSet,
  stream: Stream,
};

export const StyledSwitch = styled(Switch)`
  > label {
    margin-bottom: 0;
  }
`;

const ActionButtonsWrap = styled.span(() => css`
  float: right;
`);

const DestinationIndexSetSection = ({ indexSet, stream }: Props) => {
  const [pagination, setPagination] = useState(DEFAULT_PAGINATION);
  const archivingEnabled = indexSet.retention_strategy_class === ARCHIVE_RETENTION_STRATEGY || indexSet?.data_tiering?.archive_before_deletion;
  const { indexSets } = useStore(IndexSetsStore);
  const { data, isLoading } = useStreamOutputFilters(stream.id, 'indexer', pagination);
  /* eslint-disable no-constant-condition */
  const title = true ? 'Enabled' : 'Disabled'; // TODO use api to check if enabled
  const { data: indexSetStats, isSuccess: isStatsLoaded } = useIndexSetStats(indexSet.id);

  if (isLoading) {
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
             headerLeftSection={(
               <>
                 <StyledSwitch aria-label="Toggle index set"
                               name="toggle-indexset"
                               checked
                               label={title}
                               onChange={() => {}} />
                 <SectionCountLabel>FILTERS {data?.pagination?.total || 0}</SectionCountLabel>
               </>
             )}
             actions={(
               <IndexSetUpdateForm initialValues={{ index_set_id: indexSet.id }}
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
          <tr>
            <td>{indexSet?.title}</td>
            <td>{(isStatsLoaded && indexSetStats?.size) ? NumberUtils.formatBytes(indexSetStats.size) : 0}</td>
            <td><Timestamp dateTime={indexSet.creation_date} /></td>
            <td>
              <IndexSetArchivingCell isArchivingEnabled={archivingEnabled} streamId={stream.id} />
            </td>
            {}
            <td>
              <ActionButtonsWrap>
                <LinkContainer to={Routes.SYSTEM.INDEX_SETS.SHOW(indexSet.id)}>
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
        </tbody>
      </Table>
      {data && (<IndexSetFilters streamId={stream.id} paginatedFilters={data} onPaginationChange={onPaginationChange} />)}
    </Section>
  );
};

export default DestinationIndexSetSection;
