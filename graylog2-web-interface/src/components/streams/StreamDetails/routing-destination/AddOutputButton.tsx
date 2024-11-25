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
import { useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import styled, { css } from 'styled-components';

import { Modal, BootstrapModalWrapper, Button, SegmentedControl } from 'components/bootstrap';
import type { Stream } from 'stores/streams/StreamsStore';
import CreateOutputDropdown from 'components/outputs/CreateOutputDropdown';
import AssignOutputDropdown from 'components/outputs/AssignOutputDropdown';
import { OutputsStore, type Output } from 'stores/outputs/OutputsStore';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import useStreamOutputMutation from 'hooks/useStreamOutputMutations';
import type { AvailableOutputRequestedConfiguration, AvailableOutputTypes } from 'components/streams/useAvailableOutputTypes';
import { Icon } from 'components/common';

type Props = {
  stream: Stream
  getTypeDefinition: (type: string) => AvailableOutputRequestedConfiguration,
  availableOutputTypes: AvailableOutputTypes['types'],
  assignableOutputs: Array<Output>,
};

const SegmentedContainer = styled.div(({ theme }) => css`
  padding: ${theme.spacings.sm} ${theme.spacings.xxs};
`);

type SegmentType = 'create' | 'assign';

const AddOutputButton = ({ stream, getTypeDefinition, assignableOutputs, availableOutputTypes }: Props) => {
  const [showAddOutput, setShowAddOutput] = useState(false);
  const sendTelemetry = useSendTelemetry();
  const { addStreamOutput } = useStreamOutputMutation();
  const queryClient = useQueryClient();
  const CREATE_SEGMENT = 'create';
  const ASSIGN_SEGMENT = 'assign';
  const [currentSegment, setCurrentSegment] = useState<SegmentType>(CREATE_SEGMENT);

  const SEGMENTS = [
    {
      value: 'create' as const,
      label: 'Create new output',
    },
    {
      value: 'assign' as const,
      label: 'Assign existing output',
    },
  ];

  const onCancel = () => {
    setShowAddOutput(false);
  };

  const handleCreateOutput = (data) => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.OUTPUTS.OUTPUT_CREATED, {
      app_pathname: 'stream',
    });

    OutputsStore.save(data, (result: Output) => {
      addStreamOutput({ streamId: stream.id, outputs: { outputs: [result.id] } })
        .then(() => {
          queryClient.invalidateQueries(['outputs', 'overview']);

          onCancel();
        });

      return result;
    });
  };

  const handleAssignOutput = (outputId: string) => {
    addStreamOutput({ streamId: stream.id, outputs: { outputs: [outputId] } })
      .then(() => {
        queryClient.invalidateQueries(['outputs', 'overview']);
        onCancel();
      });
  };

  const onShowAddOutput = () => {
    setShowAddOutput(true);

    sendTelemetry(TELEMETRY_EVENT_TYPE.STREAMS.STREAM_ITEM_DATA_ROUTING_DESTINATIONS_OUTPUT_ASSIGN_OPENED, {
      app_pathname: 'stream',
    });
  };

  return (
    <>
      <Button bsStyle="default"
              bsSize="sm"
              onClick={onShowAddOutput}
              title="Edit Output">
        <Icon name="add" size="sm" /> Add output
      </Button>
      {showAddOutput && (
      <BootstrapModalWrapper showModal
                             role="alertdialog"
                             onHide={() => setShowAddOutput(false)}>
        <Modal.Header closeButton>
          <Modal.Title>Add output to stream</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <SegmentedControl<SegmentType> data={SEGMENTS}
                                         value={currentSegment}
                                         onChange={setCurrentSegment} />
          <SegmentedContainer>
            {currentSegment === CREATE_SEGMENT && (
            <CreateOutputDropdown types={availableOutputTypes}
                                  onSubmit={handleCreateOutput}
                                  getTypeDefinition={getTypeDefinition} />
            )}
            {currentSegment === ASSIGN_SEGMENT && (
            <AssignOutputDropdown outputs={assignableOutputs}
                                  onSubmit={handleAssignOutput} />
            )}
          </SegmentedContainer>
        </Modal.Body>
        <Modal.Footer>
          <Button type="button" onClick={onCancel}>
            Cancel
          </Button>
        </Modal.Footer>
      </BootstrapModalWrapper>
      )}
    </>

  );
};

export default AddOutputButton;
