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
import { useCallback, useMemo } from 'react';
import type * as Immutable from 'immutable';
import type { Permission } from 'graylog-web-plugin/plugin';

import { Row, Col } from 'components/bootstrap';
import UserNotification from 'util/UserNotification';
import Spinner from 'components/common/Spinner';
import useStreamOutputMutations from 'hooks/useStreamOutputMutations';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { isPermitted } from 'util/PermissionsMixin';
import useAvailableOutputTypes, { getOutputTypeDefinition } from 'components/streams/useAvailableOutputTypes';
import useOutputs from 'hooks/useOutputs';
import useStreamOutputs from 'hooks/useStreamOutputs';
import useOutputMutations from 'hooks/useOutputMutations';

import OutputList from './OutputList';
import CreateOutputDropdown from './CreateOutputDropdown';
import AssignOutputDropdown from './AssignOutputDropdown';

type Props = {
  streamId?: string;
  permissions: Immutable.List<Permission>;
};

const OutputsComponent = ({ streamId = undefined, permissions }: Props) => {
  const sendTelemetry = useSendTelemetry();
  const { data: types } = useAvailableOutputTypes();
  const { data: allOutputsData, refetch: refetchOutputs } = useOutputs();
  const { data: streamOutputsData, refetch: refetchStreamOutputs } = useStreamOutputs(streamId, {
    enabled: !!streamId,
  });
  const { saveOutput, updateOutput, removeOutput } = useOutputMutations();
  const { addStreamOutput, removeStreamOutput } = useStreamOutputMutations();

  const outputsData = streamId ? streamOutputsData : allOutputsData;
  const outputs = outputsData?.outputs;

  const refetchAll = useCallback(() => {
    refetchOutputs();

    if (streamId) {
      refetchStreamOutputs();
    }
  }, [refetchOutputs, refetchStreamOutputs, streamId]);

  const assignableOutputs = useMemo(() => {
    if (!streamId || !allOutputsData?.outputs || !outputs) return undefined;

    const streamOutputIds = outputs.map((output) => output.id);

    return allOutputsData.outputs
      .filter((output) => streamOutputIds.indexOf(output.id) === -1)
      .sort((output1, output2) => output1.title.localeCompare(output2.title));
  }, [streamId, allOutputsData, outputs]);

  const getTypeDefinition = useCallback(
    (typeName: string, callback: (def: any) => void) => {
      const typeDefinition = getOutputTypeDefinition(types, typeName);

      if (typeDefinition) {
        callback(typeDefinition);
      }
    },
    [types],
  );

  const _handleCreateOutput = useCallback(
    (data) => {
      sendTelemetry(TELEMETRY_EVENT_TYPE.OUTPUTS.OUTPUT_CREATED, {
        app_action_value: 'create-output',
      });

      saveOutput(data).then((result: any) => {
        if (streamId) {
          // The hook handles the success/error toast; swallow rejection to avoid an unhandled rejection.
          addStreamOutput({ streamId, outputs: { outputs: [result.id] } })
            .then(() => refetchAll())
            .catch(() => {});
        } else {
          refetchAll();
        }
      });
    },
    [saveOutput, sendTelemetry, streamId, refetchAll, addStreamOutput],
  );

  const _handleAssignOutput = useCallback(
    (outputId) => {
      sendTelemetry(TELEMETRY_EVENT_TYPE.OUTPUTS.OUTPUT_ASSIGNED, {
        app_action_value: 'assign-output',
      });

      // The hook handles the success/error toast; swallow rejection to avoid an unhandled rejection.
      addStreamOutput({ streamId, outputs: { outputs: [outputId] } })
        .then(() => refetchAll())
        .catch(() => {});
    },
    [sendTelemetry, streamId, refetchAll, addStreamOutput],
  );

  const _removeOutputGlobally = useCallback(
    (outputId) => {
      sendTelemetry(TELEMETRY_EVENT_TYPE.OUTPUTS.OUTPUT_GLOBALLY_REMOVED, {
        app_action_value: 'globally-remove-output',
      });

      // eslint-disable-next-line no-alert
      if (window.confirm('Do you really want to terminate this output?')) {
        removeOutput(outputId).then(() => {
          UserNotification.success('Output was terminated.', 'Success');
          refetchAll();
        });
      }
    },
    [removeOutput, sendTelemetry, refetchAll],
  );

  const _removeOutputFromStream = useCallback(
    (outputId: string, _streamId: string) => {
      sendTelemetry(TELEMETRY_EVENT_TYPE.OUTPUTS.OUTPUT_FROM_STREAM_REMOVED, {
        app_action_value: 'remove-output-from-stream',
      });

      // eslint-disable-next-line no-alert
      if (window.confirm('Do you really want to remove this output from the stream?')) {
        // The hook handles the success/error toast; swallow rejection to avoid an unhandled rejection.
        removeStreamOutput({ streamId: _streamId, outputId })
          .then(() => refetchAll())
          .catch(() => {});
      }
    },
    [sendTelemetry, refetchAll, removeStreamOutput],
  );

  const _handleOutputUpdate = useCallback(
    (output, deltas) => {
      sendTelemetry(TELEMETRY_EVENT_TYPE.OUTPUTS.OUTPUT_UPDATED, {
        app_action_value: 'output-update',
      });

      updateOutput({ outputId: output.id, title: output.title, deltas }).then(() => {
        refetchAll();
      });
    },
    [updateOutput, sendTelemetry, refetchAll],
  );

  if (outputs && types && (!streamId || assignableOutputs)) {
    const createOutputDropdown = isPermitted(permissions, ['outputs:create']) ? (
      <CreateOutputDropdown types={types} onSubmit={_handleCreateOutput} getTypeDefinition={getTypeDefinition} />
    ) : null;
    const assignOutputDropdown = streamId ? (
      <AssignOutputDropdown outputs={assignableOutputs} onSubmit={_handleAssignOutput} />
    ) : null;

    return (
      <div className="outputs">
        <Row className="content">
          <Col md={4}>{createOutputDropdown}</Col>
          <Col md={8}>{assignOutputDropdown}</Col>
        </Row>

        <OutputList
          streamId={streamId}
          outputs={outputs}
          getTypeDefinition={getTypeDefinition}
          types={types}
          onRemove={_removeOutputFromStream}
          onTerminate={_removeOutputGlobally}
          onUpdate={_handleOutputUpdate}
        />
      </div>
    );
  }

  return <Spinner />;
};

export default OutputsComponent;
