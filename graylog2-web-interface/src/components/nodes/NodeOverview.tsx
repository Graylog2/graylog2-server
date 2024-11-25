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
import { PluginStore } from 'graylog-web-plugin/plugin';

import { LinkContainer } from 'components/common/router';
import { Row, Col, Button } from 'components/bootstrap';
import Routes from 'routing/Routes';
import HideOnCloud from 'util/conditional/HideOnCloud';
import BufferUsage from 'components/nodes/BufferUsage';
import SystemOverviewDetails from 'components/nodes/SystemOverviewDetails';
import JvmHeapUsage from 'components/nodes/JvmHeapUsage';
import JournalDetails from 'components/nodes/JournalDetails';
import SystemInformation from 'components/nodes/SystemInformation';
import RestApiOverview from 'components/nodes/RestApiOverview';
import PluginsDataTable from 'components/nodes/PluginsDataTable';
import InputTypesDataTable from 'components/nodes/InputTypesDataTable';
import type { NodeInfo } from 'stores/nodes/NodesStore';
import type { Plugin } from 'stores/system/SystemPluginsStore';
import type { Input } from 'components/messageloaders/Types';
import type { InputDescription } from 'stores/inputs/InputTypesStore';
import type { SystemOverview } from 'stores/cluster/types';

type InputState = {
  detailed_message:string,
  id:string,
  message_input:Input
  started_at:string,
  state:string,
};
type Memory = {
  bytes: number
  kilobytes: number
  megabytes: number
}

type JvmInformation = {
  free_memory: Memory
  max_memory: Memory
  total_memory:Memory
  used_memory: Memory
  node_id: string
  pid: string
  info: string
}

type Props = {
  node: NodeInfo,
  plugins?: Array<Plugin>
  inputStates?: Array<InputState>
  inputDescriptions?: { [type: string]: InputDescription },
  jvmInformation?: JvmInformation
  systemOverview: SystemOverview,
}

const NodeOverview = ({ node, plugins, inputStates, inputDescriptions, jvmInformation, systemOverview }: Props) => {
  const DataWareHouseJournal = PluginStore.exports('dataWarehouse')?.[0]?.DataWarehouseJournal;
  const pluginCount = `${plugins?.length || 0} plugins installed`;

  const runningInputs = inputStates?.filter((inputState) => inputState.state.toUpperCase() === 'RUNNING');

  const inputCount = `${runningInputs?.length || 0} inputs running on this node`;

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
      {DataWareHouseJournal && <DataWareHouseJournal nodeId={node.node_id} />}
      <Row className="content">
        <Col md={6}>
          <h2>System</h2>
          <SystemInformation node={node} systemInformation={systemOverview} jvmInformation={jvmInformation} />
        </Col>
        <Col md={6}>
          <h2>REST API</h2>
          <RestApiOverview node={node} />
        </Col>
      </Row>

      <Row className="content">
        <Col md={12}>
          <h2>Installed plugins <small>{pluginCount}</small></h2>
          <PluginsDataTable plugins={plugins} />
        </Col>
      </Row>

      <Row className="content">
        <Col md={12}>
          <HideOnCloud>
            <span className="pull-right">
              <LinkContainer to={Routes.node_inputs(node.node_id)}>
                <Button bsStyle="success" bsSize="small">Manage inputs</Button>
              </LinkContainer>
            </span>
          </HideOnCloud>
          <h2 style={{ marginBottom: 15 }}>Available input types <small>{inputCount}</small></h2>
          <InputTypesDataTable inputDescriptions={inputDescriptions} />
        </Col>
      </Row>
    </div>
  );
};

export default NodeOverview;
