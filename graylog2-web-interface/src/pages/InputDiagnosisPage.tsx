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
import { Row, Col, Button } from 'components/bootstrap';
import useInputDiagnosis, { metricWithPrefix } from 'components/inputs/InputDiagnosis/useInputDiagnosis';
import ShowReceivedMessagesButton from 'components/inputs/InputDiagnosis/ShowReceivedMessagesButton';
import type { CounterMetric, GaugeMetric, Rate } from 'stores/metrics/MetricsStore';
import NetworkStats from 'components/inputs/InputDiagnosis/NetworkStats';

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
  margin-left:  ${theme.spacings.sm};
  margin-right:  ${theme.spacings.sm};
`);

const InfoCol = styled(Col)(({ theme }) => css`
  border: 1px solid;
  border-radius: ${theme.spacings.sm};
  padding: ${theme.spacings.sm};
`);

const MetricsCol = styled(Col)(({ theme }) => css`
  padding: ${theme.spacings.sm};
`);

const InputDiagnosisPage = () => {
  const { inputId } = useParams();
  const { input, inputStateByNode, inputDescription, metricsByNode } = useInputDiagnosis(inputId);
  console.log(input, inputStateByNode, inputDescription, metricsByNode);

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
                  <dt>This Input is listening on:</dt>
                  <dd>
                    Bind address {inputDescription?.requested_configuration?.bind_address?.default_value},
                    Port {inputDescription?.requested_configuration?.port?.default_value}.
                  </dd>
                  <dt>This Input is listening for:</dt>
                  <dd>{inputDescription?.requested_configuration?.tcp_keepalive ? 'TCP Traffic.' : 'UDP Traffic.'}</dd>
                </StyledDl>
              </InfoCol>
              <MetricsCol xs={6}>
                {metricsByNode && (
                  <StyledDl>
                    <dt>Total Messages received by Input:</dt>
                    <dd>{(metricsByNode[input.node][metricWithPrefix(input, 'incomingMessages')]?.metric as Rate)?.rate?.total || 0} events</dd>
                    <dt>Messages received (15min avg):</dt>
                    <dd>{(metricsByNode[input.node][metricWithPrefix(input, 'incomingMessages')]?.metric as Rate)?.rate?.fifteen_minute || 0} events/second</dd>
                    <dt>Empty Messages Discarded:</dt>
                    <dd>{(metricsByNode[input.node][metricWithPrefix(input, 'emptyMessages')] as CounterMetric)?.metric?.count || 0}</dd>
                    <dt>Active Connections:</dt>
                    <dd>
                      {(metricsByNode[input.node][metricWithPrefix(input, 'open_connections')] as GaugeMetric)?.metric?.value || 0}&nbsp;
                      ({(metricsByNode[input.node][metricWithPrefix(input, 'total_connections')] as GaugeMetric)?.metric?.value || 0} total)
                    </dd>
                    <dt>Network I/O:</dt>
                    <dd>
                      <NetworkStats readBytes1Sec={(metricsByNode[input.node][metricWithPrefix(input, 'read_bytes_1sec')] as GaugeMetric)?.metric?.value || 0}
                                    readBytesTotal={(metricsByNode[input.node][metricWithPrefix(input, 'read_bytes_total')] as GaugeMetric)?.metric?.value || 0}
                                    writtenBytes1Sec={(metricsByNode[input.node][metricWithPrefix(input, 'write_bytes_1sec')] as GaugeMetric)?.metric?.value || 0}
                                    writtenBytesTotal={(metricsByNode[input.node][metricWithPrefix(input, 'write_bytes_total')] as GaugeMetric)?.metric?.value || 0} />
                    </dd>
                  </StyledDl>
                )}
              </MetricsCol>
            </Row>
            <br/><br/>
            <Row>
              <Col xs={6}>
                <h3>Input Test Results</h3>
                Metrics show the last 15 minutes:
              </Col>
              <Col xs={6}>
                <Button>Every 10 seconds</Button>
              </Col>
            </Row>
            <br/><br/>
            <Row>
              <Col xs={3}>
                <div>Input State</div>
                <div>TODO</div>
              </Col>
              <Col xs={3}>
                <div>Message Error at Input</div>
                <div>TODO</div>
              </Col>
              <Col xs={3}>
                <div>Message Failed to Process</div>
                <div>TODO</div>
              </Col>
              <Col xs={3}>
                <div>Message Failed to Index</div>
                <div>TODO</div>
              </Col>
            </Row>
            <br/><br/>
            <Row>
              <Col xs={6}>
                <ShowReceivedMessagesButton input={input} />
              </Col>
              <Col xs={6}>
                <div>Recceived Message count by Stream:</div>
                <StyledDl>
                  <dt>TODO</dt>
                  <dd>TODO</dd>
                  <dt>TODO</dt>
                  <dd>TODO</dd>
                </StyledDl>
              </Col>
            </Row>
          </ContainerCol>
        </Row>
      )}
    </DocumentTitle>
  );
};

export default InputDiagnosisPage;
