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
import naturalSort from 'javascript-natural-sort';
import Immutable from 'immutable';

import { LinkContainer, Link } from 'components/graylog/router';
import { Button } from 'components/graylog';
import { DataTable, Spinner, PaginatedList, SearchForm } from 'components/common';
import { MetricContainer, CounterRate } from 'components/metrics';
import Routes from 'routing/Routes';
import CombinedProvider from 'injection/CombinedProvider';
import { useStore } from 'stores/connect';
import StreamsStore, { Stream } from 'stores/streams/StreamsStore';
import { PaginatedPipelines } from 'stores/pipelines/PipelinesStore';
import { DEFAULT_PAGINATION } from 'stores/PaginationTypes';

import PipelineConnectionsList from './PipelineConnectionsList';

const { PipelinesActions } = CombinedProvider.get('Pipelines');
const { PipelineConnectionsStore, PipelineConnectionsActions } = CombinedProvider.get('PipelineConnections');

const StyledPaginatedList = styled(PaginatedList)`
  .pagination {
    margin: 0;
  }
`;

const LoadingSpinner = styled(Spinner)(({ theme }) => css`
  margin-left: 10px;
  font-size: ${theme.fonts.size.h3};
`);

const Header = styled.div`
  display: flex;
`;

const PipelineStage = styled.div<{ $idle?: boolean }>(({ $idle, theme }) => css`
  border: 1px solid ${theme.colors.gray[$idle ? '50px' : '70px']};
  border-radius: 4px;
  display: inline-block;
  margin-right: 15px;
  padding: 20px;
  text-align: center;
  width: 120px;
  background-color: ${$idle ? theme.utils.colorLevel(theme.colors.global.contentBackground, 10) : theme.colors.global.contentBackground};
`);

const PipelineNameTD = styled.td`
  max-width: 300px;
  overflow-x: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  width: 300px;
`;

const StreamListTD = styled.td`
  max-width: 150px;
  width: 150px;
  word-wrap: break-word;
`;

const PipelineFilter = ({ onSearch }: { onSearch: (query: string) => void }) => (
  <SearchForm onSearch={onSearch}
              wrapperClass="has-bm"
              onReset={() => onSearch('')}
              topMargin={0} />
);

const _formatConnectedStreams = (streams) => {
  return streams.map((s) => s.title).join(', ');
};

const _loadPipelines = (pagination, setLoading, setPaginatedPipelines) => {
  setLoading(true);

  PipelinesActions.listPaginated(pagination).then((paginatedPipelines) => {
    setPaginatedPipelines(paginatedPipelines);
    setLoading(false);
  });
};

const ProcessingTimelineComponent = () => {
  const { connections } = useStore(PipelineConnectionsStore);
  const [streams, setStreams] = useState<Stream[] | undefined>();
  const [paginatedPipelines, setPaginatedPipelines] = useState<PaginatedPipelines|undefined>();
  const [loading, setLoading] = useState(false);
  const [pagination, setPagination] = useState(DEFAULT_PAGINATION);
  const { list: pipelines = Immutable.List(), pagination: { total = 0 } = {} } = paginatedPipelines || {};
  const { page, query, perPage } = pagination;

  useEffect(() => {
    _loadPipelines(pagination, setLoading, setPaginatedPipelines);
    PipelineConnectionsActions.list();
    StreamsStore.listStreams().then(setStreams);
  }, [pagination]);

  const isLoading = !pipelines || !streams || !connections;

  if (isLoading) {
    return <Spinner />;
  }

  const searchFilter = (
    <Header>
      <PipelineFilter onSearch={(newQuery) => setPagination({ ...pagination, query: newQuery, page: DEFAULT_PAGINATION.page })} />
      {loading && <LoadingSpinner text="" delay={0} />}
    </Header>
  );

  const _headerCellFormatter = (header) => {
    let className;

    if (header === 'Actions') {
      className = 'actions';
    }

    return <th className={className}>{header}</th>;
  };

  const _formatStages = (pipeline, stages) => {
    const stageNumbers = stages.map((stage) => stage.stage);

    return pipelines
      .map(({ stages: pStages }) => pStages.map(({ stage }) => stage))
      .reduce((usedStagesAcc: number[], pipelineStages: number[]) => {
        // Concat stages in a single array removing duplicates
        return Array.from(new Set([...usedStagesAcc, ...pipelineStages]));
      }, [])
      .sort(naturalSort)
      .map((usedStage) => {
        if (stageNumbers.indexOf(usedStage) === -1) {
          return <PipelineStage key={`${pipeline.id}-stage${usedStage}`} $idle>Idle</PipelineStage>;
        }

        return <PipelineStage key={`${pipeline.id}-stage${usedStage}`}>Stage {usedStage}</PipelineStage>;
      });
  };

  const _deletePipeline = (pipeline) => {
    return () => {
      // TODO: Replace with ConfirmDialog components
      // eslint-disable-next-line no-alert
      if (window.confirm(`Do you really want to delete pipeline "${pipeline.title}"? This action cannot be undone.`)) {
        PipelinesActions.delete(pipeline.id).then(() => {
          setPagination({ page: DEFAULT_PAGINATION.page, perPage, query });
        });
      }
    };
  };

  const _pipelineFormatter = (pipeline) => {
    const { id, title, description, stages: pStages } = pipeline;

    return (
      <tr key={id}>
        <PipelineNameTD>
          <Link to={Routes.SYSTEM.PIPELINES.PIPELINE(id)} title={title}>{title}</Link>
          <br />
          {description}
          <br />
          <MetricContainer name={`org.graylog.plugins.pipelineprocessor.ast.Pipeline.${id}.executed`}>
            <CounterRate prefix="Throughput:" suffix="msg/s" />
          </MetricContainer>
        </PipelineNameTD>
        <StreamListTD>
          <PipelineConnectionsList pipeline={pipeline}
                                   connections={connections}
                                   streams={streams}
                                   streamsFormatter={_formatConnectedStreams}
                                   noConnectionsMessage={<em>Not connected</em>} />
        </StreamListTD>
        <td>{_formatStages(pipeline, pStages)}</td>
        <td>
          <Button bsStyle="primary" bsSize="xsmall" onClick={_deletePipeline(pipeline)}>Delete</Button>
          &nbsp;
          <LinkContainer to={Routes.SYSTEM.PIPELINES.PIPELINE(id)}>
            <Button bsStyle="info" bsSize="xsmall">Edit</Button>
          </LinkContainer>
        </td>
      </tr>
    );
  };

  const headers = ['Pipeline', 'Connected to Streams', 'Processing Timeline', 'Actions'];

  return (
    <div>
      <StyledPaginatedList onChange={(newPage, newPerPage) => setPagination({ ...pagination, page: newPage, perPage: newPerPage })}
                           activePage={page}
                           totalItems={total}>
        <DataTable id="processing-timeline"
                   className="table-hover"
                   headers={headers}
                   headerCellFormatter={_headerCellFormatter}
                   rows={pipelines.toJS()}
                   customFilter={searchFilter}
                   filterKeys={[]}
                   filterLabel="Filter Pipelines"
                   dataRowFormatter={_pipelineFormatter} />
      </StyledPaginatedList>
    </div>
  );
};

ProcessingTimelineComponent.propTypes = {};

export default ProcessingTimelineComponent;
