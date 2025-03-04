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
import React from 'react';
import styled, { css } from 'styled-components';

import { isPermitted } from 'util/PermissionsMixin';
import { Link, LinkContainer } from 'components/common/router';
import Routes from 'routing/Routes';
import { CounterRate, MetricContainer } from 'components/metrics';
import PipelineConnectionsList from 'components/pipelines/PipelineConnectionsList';
import { Button, Label } from 'components/bootstrap';
import type { PipelineType } from 'stores/pipelines/PipelinesStore';
import type { PipelineConnectionsType } from 'stores/pipelines/PipelineConnectionsStore';
import type { Stream } from 'logic/streams/types';
import { defaultCompare as naturalSort } from 'logic/DefaultCompare';
import useCurrentUser from 'hooks/useCurrentUser';
import useGetPermissionsByScope from 'hooks/useScopePermissions';

import ButtonToolbar from '../bootstrap/ButtonToolbar';
import { Spinner } from '../common';

type Props = {
  pipeline: PipelineType;
  pipelines: Array<PipelineType>;
  connections: Array<PipelineConnectionsType>;
  streams: Array<Stream>;
  onDeletePipeline: () => void;
};
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

const PipelineStage = styled.div<{ $idle?: boolean }>(
  ({ $idle, theme }) => css`
    border: 1px solid ${theme.colors.gray[$idle ? '50px' : '70px']};
    border-radius: ${theme.spacings.xs};
    display: inline-block;
    margin-right: ${theme.spacings.xs};
    padding: ${theme.spacings.sm};
    text-align: center;
    width: 120px;
    background-color: ${$idle
      ? theme.utils.colorLevel(theme.colors.global.contentBackground, 10)
      : theme.colors.global.contentBackground};
  `,
);
const DefaultLabel = styled(Label)( ({ theme }) => css`
  display: inline-flex;
  margin-left: ${theme.spacings.xs};
  vertical-align: inherit;
`);

const getStagesWithoutDuplicates = (pipelineStages: Array<number>, usedStagesAcc: Array<number> = []) => Array.from(new Set([...usedStagesAcc, ...pipelineStages]));

const PipelineListItem = ({ pipeline, pipelines, connections, streams, onDeletePipeline }: Props) => {
  const currentUser = useCurrentUser();
  const { loadingScopePermissions, scopePermissions } = useGetPermissionsByScope(pipeline);
  const { id, title, description, stages } = pipeline;
  const isManaged = scopePermissions && !scopePermissions?.is_mutable;
  const isNotDeletable = scopePermissions && !scopePermissions?.is_deletable;
  const _formatConnectedStreams = (streamsUsingPipeline: Array<Stream>) =>
    streamsUsingPipeline.map((s) => s.title).join(', ');
  const _formatStages = () => {
    const stageNumbers = stages.map((stage) => stage.stage);

    return pipelines
      .map(({ stages: pipelineStages }) => pipelineStages.map(({ stage }) => stage))
      .reduce(
        (usedStagesAcc: number[], pipelineStages: number[]) =>
          getStagesWithoutDuplicates(pipelineStages, usedStagesAcc),
        [],
      )
      .sort(naturalSort)
      .map((usedStage) => {
        if (stageNumbers.indexOf(usedStage) === -1) {
          return (
            <PipelineStage key={`${pipeline.id}-stage${usedStage}`} $idle>
              Idle
            </PipelineStage>
          );
        }

        return <PipelineStage key={`${pipeline.id}-stage${usedStage}`}>Stage {usedStage}</PipelineStage>;
      });
  };
  if (loadingScopePermissions) {
    return <Spinner text="Loading pipeline..." />;
  }

  return (
    <tr key={id}>
      <PipelineNameTD>
        <Link to={Routes.SYSTEM.PIPELINES.PIPELINE(id)} title={title}>
          {title}
        </Link>
        {isManaged && (
          <DefaultLabel bsStyle="default" bsSize="xsmall">
            System managed
          </DefaultLabel>
        )}
        <br />
        {description}
        <br />
        <MetricContainer name={`org.graylog.plugins.pipelineprocessor.ast.Pipeline.${id}.executed`}>
          <CounterRate prefix="Throughput:" suffix="msg/s" />
        </MetricContainer>
      </PipelineNameTD>
      <StreamListTD>
        <PipelineConnectionsList
          pipeline={pipeline}
          connections={connections}
          streams={streams}
          streamsFormatter={_formatConnectedStreams}
          noConnectionsMessage={<em>Not connected</em>}
        />
      </StreamListTD>
      <td>{_formatStages()}</td>
      <td>
        <ButtonToolbar>
          <LinkContainer to={Routes.SYSTEM.PIPELINES.PIPELINE(id)}>
            <Button disabled={!isPermitted(currentUser.permissions, 'pipeline:edit')} bsSize="xsmall">
              Edit
            </Button>
          </LinkContainer>
          <Button
            disabled={!isPermitted(currentUser.permissions, 'pipeline:delete') || isNotDeletable}
            bsStyle="danger"
            bsSize="xsmall"
            onClick={() => onDeletePipeline()}>
            Delete
          </Button>
        </ButtonToolbar>
      </td>
    </tr>
  );
};

export default PipelineListItem;
