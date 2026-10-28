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
import { useTranslation } from 'react-i18next';
import { OrderedMap } from 'immutable';

import { Table } from 'components/bootstrap';
import { DefaultLabel, Link, PaginatedList, Spinner } from 'components/common';
import useStreams from 'components/streams/hooks/useStreams';
import Routes from 'routing/Routes';

import type { IndexSetEntity } from '../types';

const PAGE_SIZE = 10;

type Props = {
  indexSet: IndexSetEntity;
};

const AssociatedStreamsSection = ({ indexSet }: Props) => {
  const { t } = useTranslation('indexsets');
  const [page, setPage] = useState(1);
  const {
    data: { list: streams, pagination },
    isInitialLoading,
  } = useStreams({
    query: '',
    page,
    pageSize: PAGE_SIZE,
    sort: { attributeId: 'title', direction: 'asc' },
    filters: OrderedMap({ index_set_id: [indexSet.id] }),
  });

  if (isInitialLoading) {
    return <Spinner />;
  }

  const hiddenCount = indexSet.stream_count - pagination.total;
  const hiddenNote = hiddenCount > 0 && <p>{t('overview_table.hidden_streams', { count: hiddenCount })}</p>;

  if (pagination.total === 0) {
    return (
      <>
        <p>{t('overview_table.no_streams')}</p>
        {hiddenNote}
      </>
    );
  }

  return (
    <>
      {hiddenNote}
      <PaginatedList
        totalItems={pagination.total}
        pageSize={PAGE_SIZE}
        onChange={(newPage) => setPage(newPage)}
        useQueryParameter={false}
        showPageSizeSelect={false}>
        <Table bordered condensed>
          <thead>
            <tr>
              <th>{t('overview_table.streams_title')}</th>
              <th>{t('overview_table.streams_status')}</th>
            </tr>
          </thead>
          <tbody>
            {streams.map((stream) => (
              <tr key={stream.id}>
                <td>
                  <Link to={Routes.stream_view(stream.id)}>{stream.title}</Link>
                  {stream.is_default && <DefaultLabel />}
                </td>
                <td>{stream.disabled ? t('overview_table.stream_paused') : t('overview_table.stream_running')}</td>
              </tr>
            ))}
          </tbody>
        </Table>
      </PaginatedList>
    </>
  );
};

export default AssociatedStreamsSection;
