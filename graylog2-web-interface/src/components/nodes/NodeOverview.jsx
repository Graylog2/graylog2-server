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
import PropTypes from 'prop-types';
import React from 'react';

import { LinkContainer } from 'components/graylog/router';
import { Row, Col, Button } from 'components/graylog';
import Routes from 'routing/Routes';

import BufferUsage from './BufferUsage';
import SystemOverviewDetails from './SystemOverviewDetails';
import JvmHeapUsage from './JvmHeapUsage';
import JournalDetails from './JournalDetails';
import SystemInformation from './SystemInformation';
import RestApiOverview from './RestApiOverview';
import PluginsDataTable from './PluginsDataTable';
import InputTypesDataTable from './InputTypesDataTable';

class NodeOverview extends React.Component {
  static propTypes = {
    node: PropTypes.object.isRequired,
    systemOverview: PropTypes.object.isRequired,
    jvmInformation: PropTypes.object,
    plugins: PropTypes.array,
    inputDescriptions: PropTypes.object,
    inputStates: PropTypes.array,
  };

  render() {
    const { node } = this.props;
    const { systemOverview } = this.props;

    let pluginCount;

    if (this.props.plugins) {
      pluginCount = `${this.props.plugins.length} plugins installed`;
    }

    let inputCount;

    if (this.props.inputStates) {
      const runningInputs = this.props.inputStates.filter((inputState) => inputState.state.toUpperCase() === 'RUNNING');

      inputCount = `${runningInputs.length} inputs running on this node`;
    }

    return (
      <div>
        <Row className="content">
          <Col md={12}>
            <SystemOverviewDetails node={node} information={systemOverview} />
          </Col>
        </Row>

        <Row className="content">
          <Col md={12}>
            <h2 style={{ marginBottom: 5 }}>Memory/Heap usage</h2>
            <JvmHeapUsage nodeId={node.node_id} />
          </Col>
        </Row>

        <Row className="content">
          <Col md={12}>
            <h2>Buffers</h2>
            <p className="description">
              Buffers are built to cache small amounts of messages for a very short time
              (usually milliseconds) on their way through the different processors.
            </p>
            <Row>
              <Col md={4}>
                <BufferUsage nodeId={node.node_id} title="Input buffer" bufferType="input" />
              </Col>
              <Col md={4}>
                <BufferUsage nodeId={node.node_id} title="Process buffer" bufferType="process" />
              </Col>
              <Col md={4}>
                <BufferUsage nodeId={node.node_id} title="Output buffer" bufferType="output" />
              </Col>
            </Row>
          </Col>
        </Row>

        <Row className="content">
          <Col md={12}>
            <h2>Disk Journal</h2>
            <p className="description">
              Incoming messages are written to the disk journal to ensure they are kept safe in case of a server
              failure. The journal also helps keeping Graylog working if any of the outputs is too slow to keep
              up with the message rate or whenever there is a peak in incoming messages. It makes sure that
              Graylog does not buffer all of those messages in main memory and avoids overly long garbage
              collection pauses that way.
            </p>
            <JournalDetails nodeId={node.node_id} />
          </Col>
        </Row>

        <Row className="content">
          <Col md={6}>
            <h2>System</h2>
            <SystemInformation node={node} systemInformation={systemOverview} jvmInformation={this.props.jvmInformation} />
          </Col>
          <Col md={6}>
            <h2>REST API</h2>
            <RestApiOverview node={node} />
          </Col>
        </Row>

        <Row className="content">
          <Col md={12}>
            <h2>Installed plugins <small>{pluginCount}</small></h2>
            <PluginsDataTable plugins={this.props.plugins} />
          </Col>
        </Row>

        <Row className="content">
          <Col md={12}>
            <span className="pull-right">
              <LinkContainer to={Routes.node_inputs(node.node_id)}>
                <Button bsStyle="success" bsSize="small">Manage inputs</Button>
              </LinkContainer>
            </span>
            <h2 style={{ marginBottom: 15 }}>Available input types <small>{inputCount}</small></h2>
            <InputTypesDataTable inputDescriptions={this.props.inputDescriptions} />
          </Col>
        </Row>
      </div>
    );
  }
}

export default NodeOverview;
