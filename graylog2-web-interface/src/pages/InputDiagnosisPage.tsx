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
import { Alert, Button, ListGroup, ListGroupItem } from 'components/bootstrap';
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
import { DIAGNOSIS_HELP } from 'components/inputs/InputDiagnosis/Constants';
import useProductName from 'brand-customization/useProductName';
import HelpPopoverButton from 'components/common/HelpPopoverButton';

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

const InputMessage = styled.p(
  ({ theme }) => css`
    max-width: 69%;
    margin-bottom: 0;
    white-space: break-spaces;
    display: flex;

    @media (max-width: ${theme.breakpoints.max.md}) {
      max-width: 59%;
    }
  `,
);

const StyledListGroup = styled(ListGroup)(
  ({ theme }) => css`
    border: 1px solid ${theme.colors.table.row.divider};
    background-color: ${theme.colors.global.contentBackground};
    border-radius: ${theme.spacings.xs};
  `,
);

const StyledListGroupItem = styled(ListGroupItem)`
  background-color: transparent;
  display: flex;
`;

const StyledTitle = styled.p(
  ({ theme }) => css`
    font-weight: bold;
    margin-bottom: 0;
    margin-right: 1%;
    width: 30%;

    @media (max-width: ${theme.breakpoints.max.md}) {
      width: 40%;
    }
  `,
);

const StyledTitleLink = styled(Link)(
  ({ theme }) => css`
    font-weight: bold;
    margin-right: 3%;
    width: 30%;

    @media (max-width: ${theme.breakpoints.max.md}) {
      width: 40%;
    }
  `,
);

const StyledSpan = styled.span`
  padding-left: ${({ theme }) => theme.spacings.xs};
`;

const TroubleshootingContainer = styled.div`
  max-height: 400px;
  overflow-y: scroll;
`;

export const StyledList = styled.ul(
  ({ theme }) => css`
    list-style-type: disc;
    padding-left: 20px;

    li {
      margin-bottom: ${theme.spacings.xs};
    }

    ul {
      margin-top: ${theme.spacings.xs};
    }
  `,
);

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
        <StyledTitle>Node ID:</StyledTitle> <Link to={Routes.SYSTEM.CLUSTER.NODE_SHOW(nodeId)}>{nodeId}</Link>
        {detailedMessage && (
          <>
            <StyledTitle>Message:</StyledTitle>
            <InputMessage>{detailedMessage}</InputMessage>
          </>
        )}
      </StyledListGroupItem>
    );
  }

  return (
    <StyledListGroupItem key={detailedMessage}>
      {detailedMessage && (
        <>
          <StyledTitle>Message:</StyledTitle>
          <InputMessage>{detailedMessage}</InputMessage>
        </>
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
          <StyledTitle>{capitalize(state)}:</StyledTitle>
          {inputNodeStates.states[state].length}/{inputNodeStates.total} nodes
        </StyledListGroupItem>
        {inputNodeStates.states[state].map(({ detailed_message, node_id }) => (
          <NodeListItem key={node_id} detailedMessage={detailed_message} nodeId={node_id} />
        ))}
      </>
    );
  }

  return (
    <StyledListGroupItem>
      <StyledTitle>{state}:</StyledTitle>
      {inputNodeStates.states[state].length}/{inputNodeStates.total}
    </StyledListGroupItem>
  );
};

const InputDiagnosisPage = () => {
  const { inputId } = useParams();
  const { input, inputNodeStates, inputMetrics } = useInputDiagnosis(inputId);
  const navigate = useNavigate();
  const productName = useProductName();

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
          <StyledSectionGrid $columns="1fr" $rows="1fr 1fr">
            <Section
              title="Information"
              preHeaderSection={<StatusColorIndicator radius="50%" />}
              headerLeftSection={
                <HelpPopoverButton
                  helpText={`This Input Is Listening On:
                        ${DIAGNOSIS_HELP.INPUT_LISTENING_ON(productName)}
            
                        This Input is Listening For:
                        ${DIAGNOSIS_HELP.INPUT_LISTENING_FOR}
                        `}
                />
              }>
              <StyledP>The address on which the Input is being run.</StyledP>
              <StyledListGroup>
                <StyledListGroupItem>
                  <StyledTitle>Input Title:</StyledTitle>
                  {input.title}
                </StyledListGroupItem>
                <StyledListGroupItem>
                  <StyledTitle>Input Type:</StyledTitle>
                  {input.name}
                </StyledListGroupItem>
                <StyledListGroupItem>
                  <StyledTitle>This Input is running on:</StyledTitle>
                  {input.global ? `all ${productName} nodes` : <LinkToNode nodeId={input.node} />}
                </StyledListGroupItem>
                {input.attributes?.bind_address && input.attributes?.port && (
                  <>
                    <StyledListGroupItem>
                      <StyledTitle>This Input is listening on:</StyledTitle>Bind address{' '}
                      {input.attributes?.bind_address}, Port {input.attributes?.port}.
                    </StyledListGroupItem>
                    <StyledListGroupItem>
                      <StyledTitle>This Input is listening for:</StyledTitle>
                      {'tcp_keepalive' in (input.attributes || {}) ? 'TCP Traffic.' : 'UDP Traffic.'}
                    </StyledListGroupItem>
                  </>
                )}
              </StyledListGroup>
            </Section>
            <Section
              title="State"
              preHeaderSection={
                <StatusColorIndicator
                  radius="50%"
                  data-testid="state-indicator"
                  bsStyle={isInputStateDown ? 'danger' : 'success'}
                />
              }
              headerLeftSection={<HelpPopoverButton helpText={DIAGNOSIS_HELP.INPUT_STATE} />}>
              <StyledP>
                Number of {productName} nodes the Input is configured to run, and on how many it is running. If any are
                not running, click to see any associated error messages.
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
          </StyledSectionGrid>
          <Section title="Troubleshooting">
            <TroubleshootingContainer>
              <Alert>
                <p>
                  <strong>If Input is in a failed state.</strong>
                </p>
                <StyledList>
                  <li>
                    When an Input fails on one or more {productName} nodes, the Message field of the State panel will
                    show a short error message; a full length error message may be found in the {productName} server.log
                    file.
                  </li>
                  <li>
                    An input configured to use a specified port will fail if that port is privileged (and {productName}{' '}
                    is not running as root), or already in use by another Input or application.
                  </li>
                  <li>An input will fail if it is unable to route to the specified IP.</li>
                  <li>
                    An input that requires an internet connection in order to connect to an API will fail if it has no
                    internet connection or else is unable to route to that API.
                  </li>
                  <li>A TCP input will fail if it has an invalid or expired certificate.</li>
                  <li>
                    Inputs that connect to an external API (for example, the Microsoft Azure Input) require
                    configuration changes at the source to enable {productName} to collect logs. The steps required will
                    be detailed on the appropriate documentation sub-page for that Input. An input that connects to an
                    external API will fail if incorrectly configured at either the {productName} side, or (as
                    applicable) the side hosting the API.
                  </li>
                </StyledList>
                <br />
                <p>
                  <strong>If Input is running on all nodes, but messages are not reaching the Input.</strong>
                </p>
                <StyledList>
                  <li>
                    Check the Network I/O field of the Received Traffic panel. If no traffic is showing here, that
                    suggests a connectivity problem.
                    <StyledList>
                      <li>
                        If no traffic is showing, first troubleshoot network connectivity between the {productName}{' '}
                        server(s) and the log source. This may be achieved by running ping, telnet or tracert commands.
                      </li>
                      <li>
                        For Inputs that connect to an external API, check the {productName} server.log file -
                        authentication failures (invalid logins or permissions to perform the action on the API) will be
                        printed in full here.
                      </li>
                    </StyledList>
                  </li>

                  <li>
                    If traffic is showing on the Network I/O field of the Received Traffic panel, but no messages have
                    been received, this suggests the messages are not being sent in a format appropriate to the Input.
                    <StyledList>
                      <li>TCP input cannot read UDP traffic, and vice versa.</li>
                      <li>
                        A message with no content will be discarded. This can be monitored via the Empty Messages
                        Discarded field.
                      </li>
                      <li>
                        Listener Inputs expect messages in a limited range of formats and may be unable to read messages
                        in foreign formats. For troubleshooting purposes, the Raw Text Input has the most permissive
                        requirements.
                      </li>
                    </StyledList>
                  </li>
                </StyledList>
                <br />
                <p>
                  <strong>
                    If Input is running on all nodes, messages are reaching the Input, but some (or all) are showing as
                    Message Errors.
                  </strong>
                </p>
                <StyledList>
                  <li>
                    On Licensed Enterprise clusters, Failure Processing can be enabled to allow storage of messages that
                    error at each stage - input, processing, and writing to the search cluster, along with details of
                    the failure - see the failure_cause and failure_details fields. Navigate to the Message Error panel
                    and click on the message count to examine individual failed messages.
                  </li>
                </StyledList>
              </Alert>
            </TroubleshootingContainer>
          </Section>
          <StyledSectionGrid $columns="1fr" $rows="1fr 1fr">
            <Section
              preHeaderSection={
                <StatusColorIndicator radius="50%" bsStyle={hasReceivedMessageMetrics ? 'success' : 'gray'} />
              }
              headerLeftSection={
                <HelpPopoverButton
                  helpText={`Empty Messages discarded:
                ${DIAGNOSIS_HELP.EMPTY_MESSAGES_DISCARDED}

                Network I/O:
                ${DIAGNOSIS_HELP.NETWORK_IO}`}
                />
              }
              title="Received Traffic">
              <StyledP>
                Messages and network traffic that has reached the input. Note: metrics show the last 15 minutes only.
              </StyledP>
              {inputMetrics && (
                <StyledListGroup>
                  <StyledListGroupItem>
                    <StyledTitle>Total Messages received by Input:</StyledTitle>
                    {inputMetrics.incomingMessagesTotal} events
                  </StyledListGroupItem>
                  <StyledListGroupItem>
                    <StyledTitle>Empty Messages discarded:</StyledTitle>
                    {inputMetrics.emptyMessages}
                  </StyledListGroupItem>
                  {Number.isInteger(inputMetrics.open_connections) &&
                    Number.isInteger(inputMetrics.total_connections) && (
                      <StyledListGroupItem>
                        <StyledTitle>Active Connections:</StyledTitle>
                        {inputMetrics.open_connections}&nbsp; ({inputMetrics.total_connections} total)
                      </StyledListGroupItem>
                    )}
                  {Number.isInteger(inputMetrics.read_bytes_1sec) &&
                    Number.isInteger(inputMetrics.read_bytes_total) && (
                      <StyledListGroupItem>
                        <StyledTitle>Network I/O:</StyledTitle>
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
          </StyledSectionGrid>
          <Section
            preHeaderSection={<StatusColorIndicator radius="50%" bsStyle={hasReceivedMessage ? 'success' : 'gray'} />}
            title="Received Message count by Stream"
            headerLeftSection={<HelpPopoverButton helpText={DIAGNOSIS_HELP.RECEIVED_MESSAGE_COUNT_BY_STREAM} />}
            actions={<ShowReceivedMessagesButton input={input} />}>
            <StyledP>
              Messages successfully ingested from this Input in the last 15 minutes. Click on the Stream to inspect the
              messages.
            </StyledP>
            {inputMetrics.stream_message_count?.length ? (
              <StyledListGroup>
                {inputMetrics.stream_message_count.map((stream: StreamMessageCount) => (
                  <StyledListGroupItem key={stream.stream_id}>
                    <StyledTitleLink
                      to={Routes.search_with_query(`gl2_source_input:${input.id}`, 'relative', { relative: 900 }, [
                        stream.stream_id,
                      ])}>
                      <strong>{stream.stream_name}:</strong>
                    </StyledTitleLink>
                    <StyledSpan>{stream.count}</StyledSpan>
                  </StyledListGroupItem>
                ))}
              </StyledListGroup>
            ) : (
              <StyledP>
                <em>No messages from this Input were routed into Streams in the last 15 minutes.</em>
              </StyledP>
            )}
          </Section>
        </StyledSectionGrid>
      )}
    </>
  );
};

export default InputDiagnosisPage;
