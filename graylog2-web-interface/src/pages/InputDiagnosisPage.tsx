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
import capitalize from 'lodash/capitalize';

import { Icon, LinkToNode, Section } from 'components/common';
import useParams from 'routing/useParams';
import { MenuItem, Button, ListGroup, ListGroupItem } from 'components/bootstrap';
import type {
  StreamMessageCount,
  InputNodeStateInfo,
  InputNodeStates,
} from 'components/inputs/InputDiagnosis/useInputDiagnosis';
import useInputDiagnosis from 'components/inputs/InputDiagnosis/useInputDiagnosis';
import ShowReceivedMessagesButton from 'components/inputs/InputDiagnosis/ShowReceivedMessagesButton';
import NetworkStats from 'components/inputs/InputDiagnosis/NetworkStats';
import Routes from 'routing/Routes';
import { Link } from 'components/common/router';
import type { InputState } from 'stores/inputs/InputStatesStore';
import useHistory from 'routing/useHistory';
import SectionGrid from 'components/common/Section/SectionGrid';
import StatusColorIndicator from 'components/common/StatusColorIndicator';

const LeftCol = styled.div(
  ({ theme }) => css`
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    align-content: center;
    justify-content: center;
    > p {
      color: ${theme.colors.gray[50]};
    }
  `,
);

const Header = styled.div(
  ({ theme }) => css`
    display: flex;
    padding-top: ${theme.spacings.sm};
    margin-bottom: ${theme.spacings.md};
    gap: ${theme.spacings.sm};
    margin-left: -15px;
    margin-right: -15px;
    align-items: center;
  `,
);
const StyledP = styled.p(
  ({ theme }) => css`
     &&.description {
      color: ${theme.colors.gray[50]};
    }
 `,
);
const StyledSectionGrid = styled(SectionGrid)<{ $rows?: string }>(
  ({ $rows, theme }) => css`
    grid-template-rows: ${$rows || '1fr'};
    gap: ${theme.spacings.xs};
  `,
);

const InputNodeInfo = styled.div`
  max-width: 500px;
  white-space: break-spaces;
`;
const StyledListGroup = styled(ListGroup)(({ theme }) => css`
  border: 1px solid ${theme.colors.table.row.divider};
  background-color: ${theme.colors.global.contentBackground};
  border-radius: ${theme.spacings.xs};
`);
const StyledListGroupItem = styled(ListGroupItem)`
  background-color: transparent;
`;
const StyledSpan = styled.span`
  padding-left: ${({ theme }) => theme.spacings.xs}
`;
const NodeListItem = ({
  detailedMessage,
  nodeId,
}: {
  detailedMessage: InputNodeStateInfo['detailed_message'];
  nodeId: InputNodeStateInfo['node_id'];
}) => {
  if (!detailedMessage && !nodeId) return null;

  if (nodeId) {
    return (
      <StyledListGroupItem>
        <Link to={Routes.SYSTEM.NODES.SHOW(nodeId)}>
          <>
            {nodeId && (
              <>
                <b>Node ID:</b> {nodeId}
              </>
            )}
            {detailedMessage && (
              <InputNodeInfo>
                <b>Message:</b> {detailedMessage}
              </InputNodeInfo>
            )}
          </>
        </Link>
      </StyledListGroupItem>
    );
  }

  return (
    <MenuItem key={detailedMessage}>
      {detailedMessage && (
        <InputNodeInfo>
          <b>Message:</b> {detailedMessage}
        </InputNodeInfo>
      )}
    </MenuItem>
  );
};

const StateListItem = ({ inputNodeStates, state }: { inputNodeStates: InputNodeStates; state: InputState }) => {
  const showNodesList = (nodeState) => {
    const statesWithShowableInfos = inputNodeStates.states[nodeState].filter(
      (stateInfo: InputNodeStateInfo) => stateInfo.detailed_message || stateInfo.node_id,
    );

    return statesWithShowableInfos.length > 0;
  };

  if (showNodesList(state)) {
    return (
      <>
        <StyledListGroupItem>
          {capitalize(state)}: {inputNodeStates.states[state].length}/{inputNodeStates.total} nodes
        </StyledListGroupItem>
        {inputNodeStates.states[state].map(({ detailed_message, node_id }) => (
          <NodeListItem key={node_id} detailedMessage={detailed_message} nodeId={node_id} />
        ))}
      </>
    );
  }

  return (
    <p>
      {state}: {inputNodeStates.states[state].length}/{inputNodeStates.total}
    </p>
  );
};

const InputDiagnosisPage = () => {
  const { inputId } = useParams();
  const { input, inputNodeStates, inputMetrics } = useInputDiagnosis(inputId);
  const history = useHistory();

  const isInputStateDown = inputNodeStates.total === 0 || ['FAILED', 'STOPPED', 'FAILING'].some((failedState) => Object.keys(inputNodeStates.states).includes(failedState));
  const hasReceivedMessageMetrics = inputMetrics.incomingMessagesTotal > 0;
  const hasError = Object.keys(inputMetrics.message_errors).some((error) => inputMetrics.message_errors[error] > 0);
  const hasReceivedMessage = inputMetrics.stream_message_count?.some((stream) => stream.count > 0 );

  return (
    <>
      <Header>
        <Button onClick={() => history.goBack()}>
          <Icon name="arrow_left_alt" size="sm" /> Back
        </Button>
        <LeftCol>
          <h1>Input Diagnosis: {input?.name}</h1>

          <p className="description">
            Input Diagnosis can be used to test inputs and parsing without writing any data to the search cluster.
          </p>
        </LeftCol>
      </Header>
      {input && (
        <StyledSectionGrid $columns="1fr 1fr" $rows="1fr 1fr">
          <div>
            <Section title="Information" headerLeftSection={<StatusColorIndicator />}>
              <StyledP className='description'>The address on which the Input is being run.</StyledP>
              <StyledListGroup>
                <StyledListGroupItem>Input Title: {input.title}</StyledListGroupItem>
                <StyledListGroupItem>Input Type: {input.name}</StyledListGroupItem>
                <StyledListGroupItem>
                  This Input is running on: {input.global ? 'all graylog nodes' : <LinkToNode nodeId={input.node} />}
                </StyledListGroupItem>
                {input.attributes?.bind_address && input.attributes?.port && (
                  <>
                    <StyledListGroupItem>
                      This Input is listening on: Bind address {input.attributes?.bind_address}, Port{' '}
                      {input.attributes?.port}.
                    </StyledListGroupItem>
                    <StyledListGroupItem>
                      This Input is listening for:{' '}
                      {'tcp_keepalive' in (input.attributes || {}) ? 'TCP Traffic.' : 'UDP Traffic.'}
                    </StyledListGroupItem>
                  </>
                )}
              </StyledListGroup>
            </Section>
            <Section title="State" headerLeftSection={<StatusColorIndicator data-testid='state-indicator' bsStyle={isInputStateDown ? 'danger' : 'success'}/>}>
              <StyledP className='description'>Number of Graylog nodes the Input is configured to run, and on how many it is running. If any are not running, click to see any associated error messages.</StyledP>
              <StyledListGroup>
                {Object.keys(inputNodeStates.states).map((state: InputState) => (
                  <StateListItem key={state} state={state} inputNodeStates={inputNodeStates} />
                ))}
                {Object.keys(inputNodeStates.states).length === 0 && <StyledListGroupItem>Input is not running.</StyledListGroupItem>}
              </StyledListGroup>
            </Section>
          </div>
          <Section title="Troubleshooting" />
          <div>
            <Section headerLeftSection={<StatusColorIndicator bsStyle={hasReceivedMessageMetrics ? 'success' : 'gray'}/>} title="Received Traffic">
              <StyledP className='description'>Messages and network traffic that has reached the input. Note: metrics show the last 15 minutes only.</StyledP>
              {inputMetrics && (
                <StyledListGroup>
                  <StyledListGroupItem>
                    Total Messages received by Input: {inputMetrics.incomingMessagesTotal} events
                  </StyledListGroupItem>
                  <StyledListGroupItem>Empty Messages discarded: {inputMetrics.emptyMessages}</StyledListGroupItem>
                  {Number.isInteger(inputMetrics.open_connections) &&
                    Number.isInteger(inputMetrics.total_connections) && (
                      <StyledListGroupItem>
                        Active Connections: {inputMetrics.open_connections}&nbsp; ({inputMetrics.total_connections}{' '}
                        total)
                      </StyledListGroupItem>
                    )}
                  {Number.isInteger(inputMetrics.read_bytes_1sec) &&
                    Number.isInteger(inputMetrics.read_bytes_total) && (
                      <StyledListGroupItem>
                        Network I/O:
                        <NetworkStats
                          readBytes1Sec={inputMetrics.read_bytes_1sec}
                          readBytesTotal={inputMetrics.read_bytes_total}
                          writtenBytes1Sec={inputMetrics.write_bytes_1sec}
                          writtenBytesTotal={inputMetrics.write_bytes_total}
                        />
                      </StyledListGroupItem>
                    )}
                </StyledListGroup>
              )}
            </Section>
            <Section title="Message Errors" headerLeftSection={<StatusColorIndicator bsStyle={hasError ? 'danger' : 'gray'}/>}>
              <StyledP className='description'>Messages can fail to process at the Input, at the processing pipeline, or when being indexed to the Search Cluster. Click on a category to view the associated messages.</StyledP>
               <StyledListGroup>
                <StyledListGroupItem>Message Error at Input: {inputMetrics.message_errors.failures_inputs_codecs}</StyledListGroupItem>
                <StyledListGroupItem>Message failed to process: {inputMetrics.message_errors.failures_processing}</StyledListGroupItem>
                <StyledListGroupItem>Message failed to index: {inputMetrics.message_errors.failures_indexing}</StyledListGroupItem>
              </StyledListGroup>
            </Section>
          </div>
          <div>
            <Section
              title="Received Message count by Stream"
              headerLeftSection={<StatusColorIndicator bsStyle={hasReceivedMessage ? 'success' : 'gray'} />}
              actions={<ShowReceivedMessagesButton input={input} />}>
              <StyledP className='description'>Messages successfully ingested into Graylog from this Input in the last 15 minutes. Click on the Stream to inspect the messages.</StyledP>
              {inputMetrics.stream_message_count?.length && (
                <StyledListGroup>
                  {inputMetrics.stream_message_count.map((stream: StreamMessageCount) => (
                    <StyledListGroupItem key={stream.stream_id}>
                      <Link
                        to={`/search?q=gl2_source_input%3A+${input.id}&rangetype=relative&streams=${stream.stream_id}&from=900`}>
                        {stream.stream_name}:
                      </Link>
                      <StyledSpan>{stream.count}</StyledSpan>
                    </StyledListGroupItem>
                  ))}
                </StyledListGroup>
              )}
            </Section>
          </div>
        </StyledSectionGrid>
      )}
    </>
  );
};

export default InputDiagnosisPage;
