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
import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';
import styled, { css } from 'styled-components';

import { DataTable, Spinner, PaginatedList, SearchForm, QueryHelper } from 'components/common';
import useAllStreams from 'components/streams/hooks/useAllStreams';
import { DEFAULT_PAGINATION } from 'stores/PaginationTypes';
import usePaginationQueryParameter from 'hooks/usePaginationQueryParameter';
import usePipelineConnections from 'hooks/usePipelineConnections';
import usePipelineMutations from 'hooks/usePipelineMutations';
import usePipelinesPaginated from 'hooks/usePipelinesPaginated';
import { useProcessingLoadContext } from 'components/pipelines/processing-load';

import type { PipelineType } from './types';
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

const PipelineFilter = ({ query, onSearch }: { query: string; onSearch: (query: string) => void }) => {
  const { t } = useTranslation('pipelines');

  return (
    <SearchForm
      query={query}
      onSearch={onSearch}
      queryHelpComponent={<QueryHelper entityName={t('timeline.entity_name')} />}
      wrapperClass="has-bm"
      onReset={() => onSearch('')}
      topMargin={0}
    />
  );
};

// The English header texts identify the columns, the displayed texts are translated.
const HEADER_KEYS = {
  Pipeline: 'timeline.headers.pipeline',
  'Connected to Streams': 'timeline.headers.connected_streams',
  'Processing Errors': 'timeline.headers.processing_errors',
  'Pipeline Load (15m)': 'timeline.headers.pipeline_load',
  'Processing Timeline': 'timeline.headers.processing_timeline',
  Actions: 'timeline.headers.actions',
};

const ProcessingTimelineComponent = () => {
  const { t } = useTranslation('pipelines');
  const { metricsEnabled } = useProcessingLoadContext();
  const { data: connections } = usePipelineConnections();
  const { page, pageSize: perPage, resetPage, setPagination } = usePaginationQueryParameter();
  const [query, setQuery] = useState('');
  const { data: streams } = useAllStreams();
  const { deletePipeline } = usePipelineMutations();
  const { data: paginatedPipelinesData, isInitialLoading: isLoadingPipelines } = usePipelinesPaginated({
    page,
    perPage,
    query,
  });
  const { pipelines, total } = paginatedPipelinesData;

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
      {isLoadingPipelines && (
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

    return <th className={className}>{t(HEADER_KEYS[header] ?? header)}</th>;
  };

  const _deletePipeline = (pipeline: PipelineType) => {
    // TODO: Replace with ConfirmDialog components
    // eslint-disable-next-line no-alert
    if (window.confirm(t('timeline.delete_confirm', { title: pipeline.title }))) {
      deletePipeline({ pipelineId: pipeline.id }).then(() => {
        setPagination({ page: Math.max(DEFAULT_PAGINATION.page, page - 1) });
      });
    }
  };
  const dataRowFormater = (pipelineItem: PipelineType) => (
    <PipelineListItem
      pipeline={pipelineItem}
      pipelines={pipelines}
      connections={connections}
      streams={streams}
      onDeletePipeline={() => _deletePipeline(pipelineItem)}
    />
  );
  const headers = [
    'Pipeline',
    'Connected to Streams',
    'Processing Errors',
    ...(metricsEnabled ? ['Pipeline Load (15m)'] : []),
    'Processing Timeline',
    'Actions',
  ];

  return (
    <div>
      <StyledPaginatedList totalItems={total}>
        <DataTable
          id="processing-timeline"
          headers={headers}
          headerCellFormatter={_headerCellFormatter}
          rows={pipelines}
          customFilter={searchFilter}
          filterKeys={[]}
          filterLabel={t('timeline.filter_label')}
          noDataText={t('timeline.no_pipelines')}
          dataRowFormatter={dataRowFormater}
        />
      </StyledPaginatedList>
    </div>
  );
};

export default ProcessingTimelineComponent;
