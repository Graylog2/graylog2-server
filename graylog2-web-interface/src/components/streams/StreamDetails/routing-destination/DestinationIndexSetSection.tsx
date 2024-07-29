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
import styled, { css } from 'styled-components';

import { ARCHIVE_RETENTION_STRATEGY } from 'stores/indices/IndicesStore';
import { Icon, Section } from 'components/common';
import { IndexSetsStore, type IndexSet } from 'stores/indices/IndexSetsStore';
import { Table, Badge, Button, Alert } from 'components/bootstrap';
import { LinkContainer } from 'components/common/router';
import Routes from 'routing/Routes';
import { useStore } from 'stores/connect';
import type { Stream } from 'stores/streams/StreamsStore';
import IndexSetUpdateForm from 'components/streams/StreamDetails/routing-destination/IndexSetUpdateForm';
import IndexSetFilters from 'components/streams/StreamDetails/routing-destination/IndexSetFilters';

type Props = {
  indexSet: IndexSet,
  stream: Stream,
};

const ActionButtonsWrap = styled.span(() => css`
  float: right;
`);

const DestinationIndexSetSection = ({ indexSet, stream }: Props) => {
  const archivingEnabled = indexSet.retention_strategy_class === ARCHIVE_RETENTION_STRATEGY || indexSet?.data_tiering?.archive_before_deletion;
  const { indexSets } = useStore(IndexSetsStore);

  return (
    <Section title="Index Set">
      <Alert bsStyle="info">
        Messages routed to the <b>Search Cluster</b>will be searchable in Graylog and count towards Graylog License usage.<br />
        These messages will be stored in the defined Index Set until the retention policy criteria is met.<br />
        Note: Messages not routed to the <b>Search Cluster</b> will not be searchable in Graylog.
      </Alert>
      <Table>
        <thead>
          <tr>
            <td>Name</td>
            <td colSpan={2}>Archiving</td>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td>{indexSet?.title}</td>
            <td>
              <Badge bsStyle={archivingEnabled ? 'success' : 'warning'}>
                {archivingEnabled ? 'enabled' : 'disabled'}
              </Badge>
            </td>
            {/* eslint-disable-next-line jsx-a11y/control-has-associated-label */}
            <td>
              <ActionButtonsWrap>
                <LinkContainer to={Routes.SYSTEM.INDEX_SETS.SHOW(indexSet.id)}>
                  <Button bsStyle="link"
                          bsSize="xsmall"
                          onClick={() => {}}
                          title="View index set">
                    <Icon name="pageview" type="regular" />
                  </Button>
                </LinkContainer>
                <IndexSetUpdateForm initialValues={{ index_set_id: indexSet.id }}
                                    indexSets={indexSets}
                                    stream={stream} />
              </ActionButtonsWrap>
            </td>
          </tr>
        </tbody>
      </Table>
      <IndexSetFilters streamId={stream.id} />
    </Section>
  );
};

export default DestinationIndexSetSection;
