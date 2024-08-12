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
import { useParams } from 'react-router-dom';
import styled, { css } from 'styled-components';

import { defaultCompare as naturalSort } from 'logic/DefaultCompare';
import usePipelinesConnectedStream, { type StreamConnectedPipelines } from 'hooks/usePipelinesConnectedStream';
import { Table, Button, Alert } from 'components/bootstrap';
import Routes from 'routing/Routes';
import { IfPermitted, Section, Icon } from 'components/common';
import usePipelines from 'hooks/usePipelines';
import { LinkContainer } from 'components/common/router';
import StreamPipelinesConnectionForm from 'components/streams/StreamDetails/StreamPipelinesConnectionForm';

const ActionButtonsWrap = styled.span(({ theme }) => css`
  margin-right: ${theme.spacings.xxs};
  float: right;
`);

const StreamDataRoutingProcessing = () => {
  const { streamId } = useParams<{streamId: string}>();
  const { data: connectedPipelines, isInitialLoading: isLoadingConnectPipelines } = usePipelinesConnectedStream(streamId);
  const hasConnectedPipelines = !isLoadingConnectPipelines && connectedPipelines?.length > 0;
  const { data: pipelines } = usePipelines();
  const sortPipelines = (pipelinesList: StreamConnectedPipelines) => pipelinesList.sort((s1, s2) => naturalSort(s1.title, s2.title));

  return (
    <>
      <Alert bsStyle="default">
        The <b>Illuminate Processing</b> step is an immutable Pipeline that occurs before user Pipelines in the default processing order.<br />
        It collects messages that meet supported formats from the All Messages stream, parses that data into the Graylog GIM schema fields and routes them to this Stream.
      </Alert>
      <Section title="Illuminate Processing">
        <p>Illuminate Processing step</p>
      </Section>
      <Section title="Pipelines"
               actions={(
                 <IfPermitted permissions="streams:create">
                   <StreamPipelinesConnectionForm streamId={streamId}
                                                  pipelines={pipelines}
                                                  connectedPipelines={connectedPipelines} />
                 </IfPermitted>
               )}>
        <Table condensed striped hover>
          <thead>
            <tr>
              <th colSpan={2}>Pipeline</th>
            </tr>
          </thead>
          <tbody>
            {hasConnectedPipelines && sortPipelines(connectedPipelines).map((pipeline) => (
              <tr key={pipeline.id}>
                <td>
                  {pipeline.title}
                </td>
                {/* eslint-disable-next-line jsx-a11y/control-has-associated-label */}
                <td>
                  <ActionButtonsWrap className="align-right">
                    <LinkContainer to={Routes.SYSTEM.PIPELINES.PIPELINE(pipeline.id)}>
                      <Button bsStyle="default"
                              bsSize="xsmall"
                              title="View">
                        <Icon name="pageview" type="regular" />
                      </Button>
                    </LinkContainer>
                  </ActionButtonsWrap>
                </td>
              </tr>
            ))}
            {!hasConnectedPipelines && (
            <tr>
              <td colSpan={2}>This stream is not connected to any Pipeline.</td>
            </tr>
            )}
          </tbody>
        </Table>
      </Section>
    </>
  );
};

export default StreamDataRoutingProcessing;
