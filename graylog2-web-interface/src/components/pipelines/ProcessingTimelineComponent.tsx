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
import React, { useEffect, useState } from 'react';
import styled, { css } from 'styled-components';
import Immutable from 'immutable';

import { DataTable, Spinner, PaginatedList, SearchForm, QueryHelper } from 'components/common';
import { useStore } from 'stores/connect';
import type { Stream } from 'stores/streams/StreamsStore';
import StreamsStore from 'stores/streams/StreamsStore';
import type { PaginatedPipelines, PipelineType } from 'stores/pipelines/PipelinesStore';
import { PipelinesActions } from 'stores/pipelines/PipelinesStore';
import { DEFAULT_PAGINATION, type Pagination } from 'stores/PaginationTypes';
import usePaginationQueryParameter from 'hooks/usePaginationQueryParameter';
import { PipelineConnectionsStore, PipelineConnectionsActions } from 'stores/pipelines/PipelineConnectionsStore';

import PipelineListItem from './PipelineListItem';

const StyledPaginatedList = styled(PaginatedList)`
  .pagination {
    margin: 0;
  }
`;

const SpinnerWrapper = styled.div(
  ({ theme }) => css`
    font-size: ${theme.fonts.size.h3};
    padding: ${theme.spacings.xxs} ${theme.spacings.sm};
  `,
);

const Header = styled.div`
  display: flex;
`;

const PipelineFilter = ({ query, onSearch }: { query: string; onSearch: (query: string) => void }) => (
  <SearchForm
    query={query}
    onSearch={onSearch}
    queryHelpComponent={<QueryHelper entityName="Pipeline" />}
    wrapperClass="has-bm"
    onReset={() => onSearch('')}
    topMargin={0}
  />
);

const _loadPipelines = (
  pagination: Pagination,
  setLoading: (value: boolean) => void,
  setPaginatedPipelines: (pipelines: PaginatedPipelines | undefined) => void,
) => {
  setLoading(true);

  PipelinesActions.listPaginated(pagination).then((paginatedPipelines) => {
    setPaginatedPipelines(paginatedPipelines);
    setLoading(false);
  });
};

const ProcessingTimelineComponent = () => {
  const { connections } = useStore(PipelineConnectionsStore);
  const { page, pageSize: perPage, resetPage, setPagination } = usePaginationQueryParameter();
  const [query, setQuery] = useState('');
  const [streams, setStreams] = useState<Stream[] | undefined>();
  const [paginatedPipelines, setPaginatedPipelines] = useState<PaginatedPipelines | undefined>();
  const [loading, setLoading] = useState(false);
  const { list: pipelines = Immutable.List(), pagination: { total = 0 } = {} } = paginatedPipelines || {};

  useEffect(() => {
    _loadPipelines({ page, perPage, query }, setLoading, setPaginatedPipelines);
    PipelineConnectionsActions.list();
    StreamsStore.listStreams().then(setStreams);
  }, [page, perPage, query]);

  const isLoading = !pipelines || !streams || !connections;

  if (isLoading) {
    return <Spinner />;
  }

  const handleSearch = (newQuery: string) => {
    resetPage();
    setQuery(newQuery);
  };

  const searchFilter = (
    <Header>
      <PipelineFilter query={query} onSearch={handleSearch} />
      {loading && (
        <SpinnerWrapper>
          <Spinner text="" delay={0} />
        </SpinnerWrapper>
      )}
    </Header>
  );

  const _headerCellFormatter = (header: string) => {
    let className;

    if (header === 'Actions') {
      className = 'actions';
    }

    return <th className={className}>{header}</th>;
  };

  const _deletePipeline = (pipeline: PipelineType) => {
    // TODO: Replace with ConfirmDialog components
    // eslint-disable-next-line no-alert
    if (window.confirm(`Do you really want to delete pipeline "${pipeline.title}"? This action cannot be undone.`)) {
      PipelinesActions.delete(pipeline.id).then(() => {
        _loadPipelines({ page, perPage, query }, setLoading, setPaginatedPipelines);
        setPagination({ page: Math.max(DEFAULT_PAGINATION.page, page - 1) });
      });
    }
  };
  const dataRowFormater = (pipelineItem: PipelineType) => (
    <PipelineListItem
      pipeline={pipelineItem}
      pipelines={pipelines.toJS()}
      connections={connections}
      streams={streams}
      onDeletePipeline={() => _deletePipeline(pipelineItem)}
    />
  );
  const headers = ['Pipeline', 'Connected to Streams', 'Processing Timeline', 'Actions'];

  return (
    <div>
      <StyledPaginatedList totalItems={total}>
        <DataTable
          id="processing-timeline"
          className="table-hover"
          headers={headers}
          headerCellFormatter={_headerCellFormatter}
          rows={pipelines.toJS()}
          customFilter={searchFilter}
          filterKeys={[]}
          filterLabel="Filter Pipelines"
          noDataText="No pipelines have been found"
          dataRowFormatter={dataRowFormater}
        />
      </StyledPaginatedList>
    </div>
  );
};

export default ProcessingTimelineComponent;
