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
import { useEffect, useState } from 'react';
import type * as Immutable from 'immutable';

import { Row, Col } from 'components/bootstrap';
import UserNotification from 'util/UserNotification';
import Spinner from 'components/common/Spinner';
import StreamsStore from 'stores/streams/StreamsStore';
import { OutputsStore } from 'stores/outputs/OutputsStore';
import { getPathnameWithoutId } from 'util/URLUtils';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import useLocation from 'routing/useLocation';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import useOutputTypes from 'components/outputs/useOutputTypes';
import { isPermitted } from 'util/PermissionsMixin';

import OutputList from './OutputList';
import CreateOutputDropdown from './CreateOutputDropdown';
import AssignOutputDropdown from './AssignOutputDropdown';

type Props = {
  streamId?: string,
  permissions: Immutable.List<string>,
}

const OutputsComponent = ({ streamId, permissions }: Props) => {
  const location = useLocation();
  const sendTelemetry = useSendTelemetry();
  const { types } = useOutputTypes();
  const [outputs, setOutputs] = useState();
  const [assignableOutputs, setAssignableOutputs] = useState();

  const _fetchAssignableOutputs = (_outputs) => {
    OutputsStore.load((resp) => {
      const streamOutputIds = _outputs.map((output) => output.id);
      const _assignableOutputs = resp.outputs
        .filter((output) => streamOutputIds.indexOf(output.id) === -1)
        .sort((output1, output2) => output1.title.localeCompare(output2.title));

      setAssignableOutputs(_assignableOutputs);
    });
  };

  const loadData = () => {
    const callback = (resp) => {
      setOutputs(resp.outputs);

      if (streamId) {
        _fetchAssignableOutputs(resp.outputs);
      }
    };

    if (streamId) {
      OutputsStore.loadForStreamId(streamId, callback);
    } else {
      OutputsStore.load(callback);
    }
  };

  useEffect(() => {
    loadData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const _handleUpdate = () => {
    loadData();
  };

  const _handleCreateOutput = (data) => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.OUTPUTS.OUTPUT_CREATED, {
      app_pathname: getPathnameWithoutId(location.pathname),
      app_action_value: 'create-output',
    });

    OutputsStore.save(data, (result) => {
      if (streamId) {
        StreamsStore.addOutput(streamId, result.id, (response) => {
          _handleUpdate();

          return response;
        });
      } else {
        _handleUpdate();
      }

      return result;
    });
  };

  const _handleAssignOutput = (outputId) => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.OUTPUTS.OUTPUT_ASSIGNED, {
      app_pathname: getPathnameWithoutId(location.pathname),
      app_action_value: 'assign-output',
    });

    StreamsStore.addOutput(streamId, outputId, (response) => {
      _handleUpdate();

      return response;
    });
  };

  const _removeOutputGlobally = (outputId) => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.OUTPUTS.OUTPUT_GLOBALLY_REMOVED, {
      app_pathname: getPathnameWithoutId(location.pathname),
      app_action_value: 'globally-remove-output',
    });

    // eslint-disable-next-line no-alert
    if (window.confirm('Do you really want to terminate this output?')) {
      OutputsStore.remove(outputId, (response) => {
        UserNotification.success('Output was terminated.', 'Success');
        _handleUpdate();

        return response;
      });
    }
  };

  const _removeOutputFromStream = (outputId: string, _streamId: string) => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.OUTPUTS.OUTPUT_FROM_STREAM_REMOVED, {
      app_pathname: getPathnameWithoutId(location.pathname),
      app_action_value: 'remove-output-from-stream',
    });

    // eslint-disable-next-line no-alert
    if (window.confirm('Do you really want to remove this output from the stream?')) {
      StreamsStore.removeOutput(_streamId, outputId, (response) => {
        UserNotification.success('Output was removed from stream.', 'Success');
        _handleUpdate();

        return response;
      });
    }
  };

  const _handleOutputUpdate = (output, deltas) => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.OUTPUTS.OUTPUT_UPDATED, {
      app_pathname: getPathnameWithoutId(location.pathname),
      app_action_value: 'output-update',
    });

    OutputsStore.update(output, deltas, () => {
      _handleUpdate();
    });
  };

  if (outputs && types && (!streamId || assignableOutputs)) {
    const createOutputDropdown = (isPermitted(permissions, ['outputs:create'])
      ? (
        <CreateOutputDropdown types={types}
                              onSubmit={_handleCreateOutput}
                              getTypeDefinition={OutputsStore.loadAvailable} />
      ) : null);
    const assignOutputDropdown = (streamId
      ? (
        <AssignOutputDropdown outputs={assignableOutputs}
                              onSubmit={_handleAssignOutput} />
      ) : null);

    return (
      <div className="outputs">
        <Row className="content">
          <Col md={4}>
            {createOutputDropdown}
          </Col>
          <Col md={8}>
            {assignOutputDropdown}
          </Col>
        </Row>

        <OutputList streamId={streamId}
                    outputs={outputs}
                    getTypeDefinition={OutputsStore.loadAvailable}
                    types={types}
                    onRemove={_removeOutputFromStream}
                    onTerminate={_removeOutputGlobally}
                    onUpdate={_handleOutputUpdate} />
      </div>
    );
  }

  return <Spinner />;
};

export default OutputsComponent;
