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

import { defaultCompare as naturalSort } from 'logic/DefaultCompare';
import { LinkContainer, Link } from 'components/common/router';
import { Button } from 'components/bootstrap';
import { DataTable, Spinner, PaginatedList, SearchForm, QueryHelper } from 'components/common';
import { MetricContainer, CounterRate } from 'components/metrics';
import Routes from 'routing/Routes';
import { useStore } from 'stores/connect';
import type { Stream } from 'stores/streams/StreamsStore';
import StreamsStore from 'stores/streams/StreamsStore';
import type { PaginatedPipelines } from 'stores/pipelines/PipelinesStore';
import { PipelinesActions } from 'stores/pipelines/PipelinesStore';
import { DEFAULT_PAGINATION } from 'stores/PaginationTypes';
import usePaginationQueryParameter from 'hooks/usePaginationQueryParameter';
import { PipelineConnectionsStore, PipelineConnectionsActions } from 'stores/pipelines/PipelineConnectionsStore';
import useCurrentUser from 'hooks/useCurrentUser';
import { isPermitted } from 'util/PermissionsMixin';
import ButtonToolbar from 'components/bootstrap/ButtonToolbar';

import PipelineConnectionsList from './PipelineConnectionsList';

const StyledPaginatedList = styled(PaginatedList)`
  .pagination {
    margin: 0;
  }
`;

const SpinnerWrapper = styled.div(({ theme }) => css`
  font-size: ${theme.fonts.size.h3};
  padding: ${theme.spacings.xxs} ${theme.spacings.sm};
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

const PipelineFilter = ({ query, onSearch }: { query: string, onSearch: (query: string) => void }) => (
  <SearchForm query={query}
              onSearch={onSearch}
              queryWidth={400}
              queryHelpComponent={<QueryHelper entityName="Pipeline" />}
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
  const currentUser = useCurrentUser();
  const { connections } = useStore(PipelineConnectionsStore);
  const { page, pageSize: perPage, resetPage, setPage } = usePaginationQueryParameter();
  const [query, setQuery] = useState('');
  const [streams, setStreams] = useState<Stream[] | undefined>();
  const [paginatedPipelines, setPaginatedPipelines] = useState<PaginatedPipelines|undefined>();
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

  const handleSearch = (newQuery) => {
    resetPage();
    setQuery(newQuery);
  };

  const searchFilter = (
    <Header>
      <PipelineFilter query={query} onSearch={handleSearch} />
      {loading && <SpinnerWrapper><Spinner text="" delay={0} /></SpinnerWrapper>}
    </Header>
  );

  // eslint-disable-next-line react/no-unstable-nested-components
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
      .map(({ stages: pipelineStages }) => pipelineStages.map(({ stage }) => stage))
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
          _loadPipelines({ page, perPage, query }, setLoading, setPaginatedPipelines);
          setPage(Math.max(DEFAULT_PAGINATION.page, page - 1));
        });
      }
    };
  };

  // eslint-disable-next-line react/no-unstable-nested-components
  const _pipelineFormatter = (pipeline) => {
    const { id, title, description, stages } = pipeline;

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
        <td>{_formatStages(pipeline, stages)}</td>
        <td>
          <ButtonToolbar>
            <LinkContainer to={Routes.SYSTEM.PIPELINES.PIPELINE(id)}>
              <Button disabled={!isPermitted(currentUser.permissions, 'pipeline:edit')} bsSize="xsmall">Edit</Button>
            </LinkContainer>
            <Button disabled={!isPermitted(currentUser.permissions, 'pipeline:delete')} bsStyle="danger" bsSize="xsmall" onClick={_deletePipeline(pipeline)}>Delete</Button>
          </ButtonToolbar>
        </td>
      </tr>
    );
  };

  const headers = ['Pipeline', 'Connected to Streams', 'Processing Timeline', 'Actions'];

  return (
    <div>
      <StyledPaginatedList totalItems={total}>
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
