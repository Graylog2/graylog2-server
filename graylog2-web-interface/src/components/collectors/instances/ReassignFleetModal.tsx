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
import { useState, useCallback } from 'react';

import { Modal } from 'components/bootstrap';
import { Select, Spinner } from 'components/common';
import ModalSubmit from 'components/common/ModalSubmit';

import { useFleets, useCollectorsMutations } from '../hooks';
import type { Fleet } from '../types';

type Props = {
  instanceUids: string[];
  currentFleetId?: string;
  onClose: () => void;
  onSuccess?: () => void;
};

const ReassignFleetModal = ({ instanceUids, currentFleetId, onClose, onSuccess }: Props) => {
  const { data: fleets, isLoading: fleetsLoading } = useFleets();
  const { reassignInstances, isReassigningInstances } = useCollectorsMutations();
  const [selectedFleetId, setSelectedFleetId] = useState<string | undefined>(undefined);

  const availableFleets = (fleets || []).filter(
    (fleet: Fleet) => fleet.id !== currentFleetId,
  );

  const fleetOptions = availableFleets.map((fleet: Fleet) => ({
    label: fleet.name,
    value: fleet.id,
  }));

  const handleSubmit = useCallback(async () => {
    if (!selectedFleetId) return;

    await reassignInstances({ instanceUids, fleetId: selectedFleetId });
    onSuccess?.();
    onClose();
  }, [selectedFleetId, instanceUids, reassignInstances, onSuccess, onClose]);

  const instanceCount = instanceUids.length;
  const descriptor = instanceCount === 1 ? 'instance' : 'instances';

  return (
    <Modal onHide={onClose} show>
      <Modal.Header showCloseButton>
        <Modal.Title>Reassign {instanceCount} {descriptor} to fleet</Modal.Title>
      </Modal.Header>
      <Modal.Body>
        {fleetsLoading ? (
          <Spinner />
        ) : (
          <Select
            placeholder="Select a fleet..."
            options={fleetOptions}
            value={selectedFleetId}
            onChange={(value: string) => setSelectedFleetId(value)}
            clearable={false}
          />
        )}
      </Modal.Body>
      <Modal.Footer>
        <ModalSubmit
          isAsyncSubmit
          submitButtonText={`Reassign ${descriptor}`}
          submitLoadingText="Reassigning..."
          onCancel={onClose}
          onSubmit={handleSubmit}
          disabledSubmit={!selectedFleetId}
          isSubmitting={isReassigningInstances}
        />
      </Modal.Footer>
    </Modal>
  );
};

export default ReassignFleetModal;
