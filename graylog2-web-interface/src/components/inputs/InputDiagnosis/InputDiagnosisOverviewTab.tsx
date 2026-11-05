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
import { useTranslation } from 'react-i18next';

import { LinkToNode, RelativeTime, Section, Link } from 'components/common';
import { Alert, ListGroup, ListGroupItem } from 'components/bootstrap';
import type {
  StreamMessageCount,
  InputNodeStateInfo,
  InputNodeStates,
  InputDiagnosisMetrics,
} from 'components/inputs/InputDiagnosis/useInputDiagnosis';
import ShowReceivedMessagesButton from 'components/inputs/InputDiagnosis/ShowReceivedMessagesButton';
import NetworkStats from 'components/inputs/InputDiagnosis/NetworkStats';
import Routes from 'routing/Routes';
import type { InputState } from 'hooks/useInputsStates';
import type { Input } from 'components/messageloaders/Types';
import SectionGrid from 'components/common/Section/SectionGrid';
import StatusColorIndicator from 'components/common/StatusColorIndicator';
import DiagnosisMessageErrors from 'components/inputs/InputDiagnosis/DiagnosisMessageErrors';
import HelpPopoverButton from 'components/common/HelpPopoverButton';

const StyledListGroupItem = styled(ListGroupItem)`
  display: flex;
`;

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

const NodeListItemContent = styled.div(
  ({ theme }) => css`
    display: flex;
    flex-direction: column;
    gap: ${theme.spacings.xs};
    width: 100%;
  `,
);

const NodeDetailsRow = styled.div`
  display: flex;
  align-items: flex-start;
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
  lastFailedAt = undefined,
  nodeId,
}: {
  detailedMessage: InputNodeStateInfo['detailed_message'];
  lastFailedAt?: InputNodeStateInfo['last_failed_at'];
  nodeId: InputNodeStateInfo['node_id'];
}) => {
  const { t } = useTranslation('inputs');

  if (!detailedMessage && !nodeId && !lastFailedAt) return null;

  return (
    <StyledListGroupItem key={detailedMessage}>
      <NodeListItemContent>
        {nodeId && (
          <NodeDetailsRow>
            <StyledTitle>{t('diagnosis.node.node_id')}</StyledTitle>
            <Link to={Routes.SYSTEM.CLUSTER.NODE_SHOW(nodeId)}>{nodeId}</Link>
          </NodeDetailsRow>
        )}
        {detailedMessage && (
          <NodeDetailsRow>
            <StyledTitle>{t('diagnosis.node.message')}</StyledTitle>
            <InputMessage>{detailedMessage}</InputMessage>
          </NodeDetailsRow>
        )}
        {lastFailedAt && (
          <NodeDetailsRow>
            <StyledTitle>{t('diagnosis.node.last_failed')}</StyledTitle>
            <RelativeTime dateTime={lastFailedAt} />
          </NodeDetailsRow>
        )}
      </NodeListItemContent>
    </StyledListGroupItem>
  );
};

const StateListItem = ({ inputNodeStates, state }: { inputNodeStates: InputNodeStates; state: InputState }) => {
  const { t } = useTranslation('inputs');
  const showNodesList = (nodeState: InputState) => {
    const statesWithShowableInfos = inputNodeStates.states[nodeState].filter(
      (stateInfo: InputNodeStateInfo) => stateInfo.detailed_message || stateInfo.node_id,
    );

    return statesWithShowableInfos.length > 0;
  };

  if (showNodesList(state)) {
    return (
      <>
        <StyledListGroupItem>
          <StyledTitle>{t('diagnosis.node.state_label', { state: capitalize(state) })}</StyledTitle>
          {t('diagnosis.node.state_nodes', {
            count: inputNodeStates.states[state].length,
            total: inputNodeStates.total,
          })}
        </StyledListGroupItem>
        {inputNodeStates.states[state].map(({ detailed_message, last_failed_at, node_id }) => (
          <NodeListItem
            key={node_id}
            detailedMessage={detailed_message}
            lastFailedAt={last_failed_at}
            nodeId={node_id}
          />
        ))}
      </>
    );
  }

  return (
    <StyledListGroupItem>
      <StyledTitle>{t('diagnosis.node.state_label', { state })}</StyledTitle>
      {t('diagnosis.node.state_count', { count: inputNodeStates.states[state].length, total: inputNodeStates.total })}
    </StyledListGroupItem>
  );
};

type Props = {
  input: Input;
  inputId: string;
  inputNodeStates: InputNodeStates;
  inputMetrics: InputDiagnosisMetrics;
  productName: string;
  listeningProtocol: string | undefined;
  isInputStateDown: boolean;
  hasReceivedMessageMetrics: boolean;
  hasReceivedMessage: boolean;
};

const InputDiagnosisOverviewTab = ({
  input,
  inputId,
  inputNodeStates,
  inputMetrics,
  productName,
  listeningProtocol,
  isInputStateDown,
  hasReceivedMessageMetrics,
  hasReceivedMessage,
}: Props) => {
  const { t } = useTranslation('inputs');

  return (
    <StyledSectionGrid $columns="1fr 1fr" $rows="1fr 1fr">
      <StyledSectionGrid $columns="1fr" $rows="1fr 1fr">
        <Section
          title={t('diagnosis.information.title')}
          preHeaderSection={<StatusColorIndicator radius="50%" />}
          headerLeftSection={
            <HelpPopoverButton
              helpText={`${t('diagnosis.help.listening_on_title')}
${t('diagnosis.help.listening_on', { productName })}

${t('diagnosis.help.listening_for_title')}
${t('diagnosis.help.listening_for')}`}
            />
          }>
          <StyledP>{t('diagnosis.information.description')}</StyledP>
          <StyledListGroup>
            <StyledListGroupItem>
              <StyledTitle>{t('diagnosis.information.input_title')}</StyledTitle>
              {input.title}
            </StyledListGroupItem>
            <StyledListGroupItem>
              <StyledTitle>{t('diagnosis.information.input_type')}</StyledTitle>
              {input.name}
            </StyledListGroupItem>
            <StyledListGroupItem>
              <StyledTitle>{t('diagnosis.information.running_on')}</StyledTitle>
              {input.global ? (
                t('diagnosis.information.all_nodes', { productName })
              ) : (
                <LinkToNode nodeId={input.node} />
              )}
            </StyledListGroupItem>
            {input.attributes?.bind_address && input.attributes?.port && (
              <>
                <StyledListGroupItem>
                  <StyledTitle>{t('diagnosis.information.listening_on')}</StyledTitle>
                  {t('diagnosis.information.listening_on_value', {
                    bindAddress: input.attributes?.bind_address,
                    port: input.attributes?.port,
                  })}
                </StyledListGroupItem>
                {listeningProtocol && (
                  <StyledListGroupItem>
                    <StyledTitle>{t('diagnosis.information.listening_for')}</StyledTitle>
                    {listeningProtocol}
                  </StyledListGroupItem>
                )}
              </>
            )}
          </StyledListGroup>
        </Section>
        <Section
          title={t('diagnosis.state.title')}
          preHeaderSection={
            <StatusColorIndicator
              radius="50%"
              data-testid="state-indicator"
              bsStyle={isInputStateDown ? 'danger' : 'success'}
            />
          }
          headerLeftSection={<HelpPopoverButton helpText={t('diagnosis.help.input_state')} />}>
          <StyledP>{t('diagnosis.state.description', { productName })}</StyledP>
          <StyledListGroup>
            {inputMetrics.failedStarts15mCount !== undefined && (
              <StyledListGroupItem>
                <StyledTitle>{t('diagnosis.state.failed_starts')}</StyledTitle>
                {inputMetrics.failedStarts15mCount}
              </StyledListGroupItem>
            )}
            {Object.keys(inputNodeStates.states).map((state: InputState) => (
              <StateListItem key={state} state={state} inputNodeStates={inputNodeStates} />
            ))}
            {Object.keys(inputNodeStates.states).length === 0 && (
              <StyledListGroupItem>{t('diagnosis.state.not_running')}</StyledListGroupItem>
            )}
          </StyledListGroup>
        </Section>
      </StyledSectionGrid>
      <Section title={t('diagnosis.troubleshooting.title')}>
        <TroubleshootingContainer>
          <Alert>
            <p>
              <strong>{t('diagnosis.troubleshooting.failed_title')}</strong>
            </p>
            <StyledList>
              <li>{t('diagnosis.troubleshooting.failed_message_field', { productName })}</li>
              <li>{t('diagnosis.troubleshooting.failed_port', { productName })}</li>
              <li>{t('diagnosis.troubleshooting.failed_route')}</li>
              <li>{t('diagnosis.troubleshooting.failed_internet')}</li>
              <li>{t('diagnosis.troubleshooting.failed_certificate')}</li>
              <li>{t('diagnosis.troubleshooting.failed_external_api', { productName })}</li>
            </StyledList>
            <br />
            <p>
              <strong>{t('diagnosis.troubleshooting.no_messages_title')}</strong>
            </p>
            <StyledList>
              <li>
                {t('diagnosis.troubleshooting.no_messages_network_io')}
                <StyledList>
                  <li>{t('diagnosis.troubleshooting.no_messages_connectivity', { productName })}</li>
                  <li>{t('diagnosis.troubleshooting.no_messages_external_api', { productName })}</li>
                </StyledList>
              </li>

              <li>
                {t('diagnosis.troubleshooting.no_messages_format')}
                <StyledList>
                  <li>{t('diagnosis.troubleshooting.no_messages_protocol')}</li>
                  <li>{t('diagnosis.troubleshooting.no_messages_empty')}</li>
                  <li>{t('diagnosis.troubleshooting.no_messages_listener')}</li>
                </StyledList>
              </li>
            </StyledList>
            <br />
            <p>
              <strong>{t('diagnosis.troubleshooting.message_errors_title')}</strong>
            </p>
            <StyledList>
              <li>{t('diagnosis.troubleshooting.message_errors_failure_processing')}</li>
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
              helpText={`${t('diagnosis.help.empty_messages_discarded_title')}
${t('diagnosis.help.empty_messages_discarded')}

${t('diagnosis.help.network_io_title')}
${t('diagnosis.help.network_io')}`}
            />
          }
          title={t('diagnosis.received_traffic.title')}>
          <StyledP>{t('diagnosis.received_traffic.description')}</StyledP>
          {inputMetrics && (
            <StyledListGroup>
              <StyledListGroupItem>
                <StyledTitle>{t('diagnosis.received_traffic.total_messages')}</StyledTitle>
                {t('diagnosis.received_traffic.events', { count: inputMetrics.incomingMessagesTotal })}
              </StyledListGroupItem>
              <StyledListGroupItem>
                <StyledTitle>{t('diagnosis.received_traffic.empty_messages')}</StyledTitle>
                {inputMetrics.emptyMessages}
              </StyledListGroupItem>
              {Number.isInteger(inputMetrics.open_connections) && Number.isInteger(inputMetrics.total_connections) && (
                <StyledListGroupItem>
                  <StyledTitle>{t('diagnosis.received_traffic.active_connections')}</StyledTitle>
                  {t('diagnosis.received_traffic.connections_value', {
                    open: inputMetrics.open_connections,
                    total: inputMetrics.total_connections,
                  })}
                </StyledListGroupItem>
              )}
              {Number.isInteger(inputMetrics.read_bytes_1sec) && Number.isInteger(inputMetrics.read_bytes_total) && (
                <StyledListGroupItem>
                  <StyledTitle>{t('diagnosis.received_traffic.network_io')}</StyledTitle>
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
        title={t('diagnosis.message_count_by_stream.title')}
        headerLeftSection={<HelpPopoverButton helpText={t('diagnosis.help.received_message_count_by_stream')} />}
        actions={<ShowReceivedMessagesButton input={input} />}>
        <StyledP>{t('diagnosis.message_count_by_stream.description')}</StyledP>
        {inputMetrics.stream_message_count?.length ? (
          <StyledListGroup>
            {inputMetrics.stream_message_count.map((stream: StreamMessageCount) => (
              <StyledListGroupItem key={stream.stream_id}>
                <StyledTitleLink
                  to={Routes.search_with_query(`gl2_source_input:${input.id}`, 'relative', { relative: 900 }, [
                    stream.stream_id,
                  ])}>
                  <strong>{t('diagnosis.message_count_by_stream.stream', { streamName: stream.stream_name })}</strong>
                </StyledTitleLink>
                <StyledSpan>{stream.count}</StyledSpan>
              </StyledListGroupItem>
            ))}
          </StyledListGroup>
        ) : (
          <StyledP>
            <em>{t('diagnosis.message_count_by_stream.none')}</em>
          </StyledP>
        )}
      </Section>
    </StyledSectionGrid>
  );
};

export default InputDiagnosisOverviewTab;
