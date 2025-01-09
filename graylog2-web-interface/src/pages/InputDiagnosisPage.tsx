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

import { DocumentTitle, LinkToNode, PageHeader } from 'components/common';
import useParams from 'routing/useParams';
import { Row, Col, DropdownButton, MenuItem } from 'components/bootstrap';
import useInputDiagnosis from 'components/inputs/InputDiagnosis/useInputDiagnosis';
import ShowReceivedMessagesButton from 'components/inputs/InputDiagnosis/ShowReceivedMessagesButton';
import NetworkStats from 'components/inputs/InputDiagnosis/NetworkStats';
import Routes from 'routing/Routes';
import { LinkContainer } from 'components/common/router';

const StyledDl = styled.dl`
  margin: 0;

  dt {
    float: left;
    clear: left;
  }

  dd {
    margin-left: 260px;
  }
`;

const ContainerCol = styled(Col)(({ theme }) => css`
  margin-left: ${theme.spacings.sm};
  margin-right: ${theme.spacings.sm};
`);

const InfoCol = styled(Col)(({ theme }) => css`
  border: 1px solid;
  border-radius: ${theme.spacings.sm};
  padding: ${theme.spacings.sm};
`);

const MetricsCol = styled(Col)(({ theme }) => css`
  padding: ${theme.spacings.sm};
`);

const InputNodeInfo = styled.div`
  max-width: 500px;
  white-space: break-spaces;
`;

const InputDiagnosisPage = () => {
  const { inputId } = useParams();
  const { input, inputNodeStates, inputMetrics } = useInputDiagnosis(inputId);

  return (
    <DocumentTitle title="Input Diagnosis">
      <PageHeader title="Input Diagnosis">
        <span>Input Diagnosis can be used to test inputs and parsing without writing any data to the search cluster.</span>
      </PageHeader>
      {input && (
        <Row className="content">
          <ContainerCol xs={12}>
            <Row>
              <InfoCol xs={6}>
                <StyledDl>
                  <dt>Input Title:</dt>
                  <dd>{input.title}</dd>
                  <dt>Input Type:</dt>
                  <dd>{input.name}</dd>
                  <dt>This Input is running on:</dt>
                  <dd>{input.global ? 'all graylog nodes' : <LinkToNode nodeId={input.node} />}</dd>
                  {input.attributes?.bind_address && input.attributes?.port && (
                    <>
                      <dt>This Input is listening on:</dt>
                      <dd>
                        Bind address {input.attributes?.bind_address},
                        Port {input.attributes?.port}.
                      </dd>
                      <dt>This Input is listening for:</dt>
                      <dd>{('tcp_keepalive' in (input.attributes || {})) ? 'TCP Traffic.' : 'UDP Traffic.'}</dd>
                    </>
                  )}
                </StyledDl>
              </InfoCol>
              <MetricsCol xs={6}>
                {inputMetrics && (
                  <StyledDl>
                    <dt>Total Messages received by Input:</dt>
                    <dd>{inputMetrics.incomingMessagesTotal} events</dd>
                    <dt>Empty Messages discarded:</dt>
                    <dd>{inputMetrics.emptyMessages}</dd>
                    {Number.isInteger(inputMetrics.open_connections) && Number.isInteger(inputMetrics.total_connections) && (
                      <>
                        <dt>Active Connections:</dt>
                        <dd>
                          {inputMetrics.open_connections}&nbsp;
                          ({inputMetrics.total_connections} total)
                        </dd>
                      </>
                    )}
                    {Number.isInteger(inputMetrics.read_bytes_1sec) && Number.isInteger(inputMetrics.read_bytes_total) && (
                      <>
                        <dt>Network I/O:</dt>
                        <dd>
                          <NetworkStats readBytes1Sec={inputMetrics.read_bytes_1sec}
                                        readBytesTotal={inputMetrics.read_bytes_total}
                                        writtenBytes1Sec={inputMetrics.write_bytes_1sec}
                                        writtenBytesTotal={inputMetrics.write_bytes_total} />
                        </dd>
                      </>
                    )}
                  </StyledDl>
                )}
              </MetricsCol>
            </Row>
            <br /><br />
            <Row>
              <Col xs={6}>
                <h3>Input Test Results</h3>
                Metrics show the last 15 minutes:
              </Col>
            </Row>
            <br /><br />
            <Row>
              <Col xs={3}>
                <dt>Input State</dt>
                {Object.keys(inputNodeStates.states).map((state) => (
                  <DropdownButton title={<dd key={state}>{state.toLowerCase()}: {inputNodeStates.states[state].length}/{inputNodeStates.total}</dd>}
                                  key={state}
                                  bsSize="xs">
                    {inputNodeStates.states[state].map(({ detailed_message, node_id }) => (
                      <LinkContainer key={node_id} to={Routes.SYSTEM.NODES.SHOW(node_id)}>
                        <MenuItem>
                          {node_id && (
                            <div><b>Node ID:</b> {node_id}</div>
                          )}
                          {detailed_message && (
                            <InputNodeInfo><b>Message:</b> {detailed_message}</InputNodeInfo>
                          )}
                        </MenuItem>
                      </LinkContainer>
                    ))}
                  </DropdownButton>
                ))}
              </Col>
              <Col xs={3}>
                <dt>Message Error at Input</dt>
                <dd>{inputMetrics.failures_inputs_codecs}</dd>
              </Col>
              <Col xs={3}>
                <dt>Message Failed to process</dt>
                <dd>{inputMetrics.failures_processing}</dd>
              </Col>
              <Col xs={3}>
                <dt>Message Failed to index</dt>
                <dd>{inputMetrics.failures_indexing}</dd>
              </Col>
            </Row>
            <br /><br />
            <Row>
              <Col xs={6}>
                <h3>Received Message count by Stream</h3>
                {inputMetrics.stream_message_count?.length && (
                  <StyledDl>
                    {inputMetrics.stream_message_count.map(([key, value]) => (
                      <span key={key}>
                        <dt>{key}</dt>
                        <dd>{value}</dd>
                      </span>
                    ))}
                  </StyledDl>
                )}
              </Col>
              <Col xs={6}>
                <ShowReceivedMessagesButton input={input} />
              </Col>
            </Row>
          </ContainerCol>
        </Row>
      )}
    </DocumentTitle>
  );
};

export default InputDiagnosisPage;
