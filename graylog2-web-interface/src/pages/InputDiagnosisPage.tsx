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
import { useNavigate } from 'react-router-dom';

import { Icon, LinkToNode, Section } from 'components/common';
import useParams from 'routing/useParams';
import { Button, ListGroup, ListGroupItem } from 'components/bootstrap';
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
import SectionGrid from 'components/common/Section/SectionGrid';
import StatusColorIndicator from 'components/common/StatusColorIndicator';
import DiagnosisMessageErrors from 'components/inputs/InputDiagnosis/DiagnosisMessageErrors';
import DiagnosisHelp from 'components/inputs/InputDiagnosis/DiagnosisHelp';
import { DIAGNOSIS_HELP } from 'components/inputs/InputDiagnosis/Constants';

const LeftCol = styled.div(
  ({ theme }) => css`
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    place-content: center center;

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
const StyledListGroup = styled(ListGroup)(
  ({ theme }) => css`
    border: 1px solid ${theme.colors.table.row.divider};
    background-color: ${theme.colors.global.contentBackground};
    border-radius: ${theme.spacings.xs};
  `,
);
const StyledListGroupItem = styled(ListGroupItem)`
  background-color: transparent;
`;
const StyledSpan = styled.span`
  padding-left: ${({ theme }) => theme.spacings.xs};
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
                <strong>Node ID:</strong> {nodeId}
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
    <StyledListGroupItem key={detailedMessage}>
      {detailedMessage && (
        <InputNodeInfo>
          <strong>Message:</strong> {detailedMessage}
        </InputNodeInfo>
      )}
    </StyledListGroupItem>
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
          <strong>{capitalize(state)}</strong>: {inputNodeStates.states[state].length}/{inputNodeStates.total} nodes
        </StyledListGroupItem>
        {inputNodeStates.states[state].map(({ detailed_message, node_id }) => (
          <NodeListItem key={node_id} detailedMessage={detailed_message} nodeId={node_id} />
        ))}
      </>
    );
  }

  return (
    <StyledListGroupItem>
      <strong>{state}</strong>: {inputNodeStates.states[state].length}/{inputNodeStates.total}
    </StyledListGroupItem>
  );
};

const InputDiagnosisPage = () => {
  const { inputId } = useParams();
  const { input, inputNodeStates, inputMetrics } = useInputDiagnosis(inputId);
  const navigate = useNavigate();

  const isInputStateDown =
    inputNodeStates.total === 0 ||
    ['FAILED', 'STOPPED', 'FAILING'].some((failedState) => Object.keys(inputNodeStates.states).includes(failedState));
  const hasReceivedMessageMetrics = inputMetrics.incomingMessagesTotal > 0;
  const hasReceivedMessage = inputMetrics.stream_message_count?.some((stream) => stream.count > 0);

  return (
    <>
      <Header>
        <Button onClick={() => navigate(Routes.SYSTEM.INPUTS)}>
          <Icon name="arrow_left_alt" size="sm" /> Back
        </Button>
        <LeftCol>
          <h1>Input Diagnosis: {input?.name}</h1>

          <p>Input Diagnosis can be used to test inputs and parsing without writing any data to the search cluster.</p>
        </LeftCol>
      </Header>
      {input && (
        <StyledSectionGrid $columns="1fr 1fr" $rows="1fr 1fr">
          <div>
            <Section title="Information" headerLeftSection={<StatusColorIndicator />}>
              <StyledP>The address on which the Input is being run.</StyledP>
              <StyledListGroup>
                <StyledListGroupItem><strong>Input Title</strong>: {input.title}</StyledListGroupItem>
                <StyledListGroupItem><strong>Input Type</strong>: {input.name}</StyledListGroupItem>
                <StyledListGroupItem>
                  <strong>This Input is running on</strong> : {input.global ? 'all graylog nodes' : <LinkToNode nodeId={input.node} />}
                </StyledListGroupItem>
                {input.attributes?.bind_address && input.attributes?.port && (
                  <>
                    <StyledListGroupItem>
                      <DiagnosisHelp helpText={DIAGNOSIS_HELP.INPUT_LISTENING_ON}><strong>This Input is listening on</strong></DiagnosisHelp>: Bind
                      address {input.attributes?.bind_address}, Port {input.attributes?.port}.
                    </StyledListGroupItem>
                    <StyledListGroupItem>
                     <DiagnosisHelp helpText={DIAGNOSIS_HELP.INPUT_LISTENING_FOR}> <strong>This Input is listening for</strong></DiagnosisHelp>:{' '}
                      {'tcp_keepalive' in (input.attributes || {}) ? 'TCP Traffic.' : 'UDP Traffic.'}
                    </StyledListGroupItem>
                  </>
                )}
              </StyledListGroup>
            </Section>
            <Section
              title="State"
              headerLeftSection={
                <>
                  <StatusColorIndicator
                    data-testid="state-indicator"
                    bsStyle={isInputStateDown ? 'danger' : 'success'}
                  />
                  <DiagnosisHelp helpText={DIAGNOSIS_HELP.INPUT_STATE} />
                </>
              }>
              <StyledP>
                Number of Graylog nodes the Input is configured to run, and on how many it is running. If any are not
                running, click to see any associated error messages.
              </StyledP>
              <StyledListGroup>
                {Object.keys(inputNodeStates.states).map((state: InputState) => (
                  <StateListItem key={state} state={state} inputNodeStates={inputNodeStates} />
                ))}
                {Object.keys(inputNodeStates.states).length === 0 && (
                  <StyledListGroupItem>Input is not running.</StyledListGroupItem>
                )}
              </StyledListGroup>
            </Section>
          </div>
          <Section title="Troubleshooting" />
          <div>
            <Section
              headerLeftSection={<StatusColorIndicator bsStyle={hasReceivedMessageMetrics ? 'success' : 'gray'} />}
              title="Received Traffic">
              <StyledP>
                Messages and network traffic that has reached the input. Note: metrics show the last 15 minutes only.
              </StyledP>
              {inputMetrics && (
                <StyledListGroup>
                  <StyledListGroupItem>
                    <strong>Total Messages received by Input</strong>: {inputMetrics.incomingMessagesTotal} events
                  </StyledListGroupItem>
                  <StyledListGroupItem>
                   <DiagnosisHelp helpText={DIAGNOSIS_HELP.EMPTY_MESSAGES_DISCARDED}><strong>Empty Messages discarded</strong></DiagnosisHelp>:{' '}
                    {inputMetrics.emptyMessages}
                  </StyledListGroupItem>
                  {Number.isInteger(inputMetrics.open_connections) &&
                    Number.isInteger(inputMetrics.total_connections) && (
                      <StyledListGroupItem>
                       <strong>Active Connections</strong>: {inputMetrics.open_connections}&nbsp; ({inputMetrics.total_connections}{' '}
                        total)
                      </StyledListGroupItem>
                    )}
                  {Number.isInteger(inputMetrics.read_bytes_1sec) &&
                    Number.isInteger(inputMetrics.read_bytes_total) && (
                      <StyledListGroupItem>
                       <DiagnosisHelp helpText={DIAGNOSIS_HELP.NETWORK_IO}><strong>Network I/O</strong></DiagnosisHelp>:
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
            <DiagnosisMessageErrors messageErrors={inputMetrics.message_errors} inputId={inputId} />
          </div>
          <div>
            <Section
              title="Received Message count by Stream"
              headerLeftSection={
                <>
                  <StatusColorIndicator bsStyle={hasReceivedMessage ? 'success' : 'gray'} />
                  <DiagnosisHelp helpText={DIAGNOSIS_HELP.RECEIVED_MESSAGE_COUNT_BY_STREAM} />
                </>
              }
              actions={<ShowReceivedMessagesButton input={input} />}>
              <StyledP>
                Messages successfully ingested into Graylog from this Input in the last 15 minutes. Click on the Stream
                to inspect the messages.
              </StyledP>
              {inputMetrics.stream_message_count?.length && (
                <StyledListGroup>
                  {inputMetrics.stream_message_count.map((stream: StreamMessageCount) => (
                    <StyledListGroupItem key={stream.stream_id}>
                      <Link
                        to={`/search?q=gl2_source_input%3A+${input.id}&rangetype=relative&streams=${stream.stream_id}&from=900`}>
                       <strong>{stream.stream_name}</strong>:
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
