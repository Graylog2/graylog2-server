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
import { useCallback } from 'react';
import { Formik, Form } from 'formik';

import { Modal } from 'components/bootstrap';
import { Select, Spinner } from 'components/common';
import ModalSubmit from 'components/common/ModalSubmit';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

import { useFleets, useCollectorsMutations, useCollectorPermissions } from '../hooks';
import useSendCollectorsTelemetry from '../hooks/useSendCollectorsTelemetry';
import type { Fleet } from '../types';

// Which entry point opened the modal. This is deliberately *not* derived from
// `instanceUids.length`: reassigning one instance from its row and running the bulk
// action that happens to have one instance selected are different user actions, and
// only the row path knows the instance's current fleet.
type Origin = 'row' | 'bulk-selection';

type Props = {
  instanceUids: string[];
  origin: Origin;
  currentFleetId?: string;
  onClose: () => void;
  onSuccess?: () => void;
};

type FormValues = {
  fleetId: string;
};

const validate = (values: FormValues) => {
  const errors: Partial<Record<keyof FormValues, string>> = {};

  if (!values.fleetId) {
    errors.fleetId = 'Fleet is required';
  }

  return errors;
};

const ReassignFleetModal = ({
  instanceUids,
  origin,
  currentFleetId = undefined,
  onClose,
  onSuccess = () => {},
}: Props) => {
  const { data: fleets, isLoading: fleetsLoading } = useFleets();
  const { reassignInstances } = useCollectorsMutations();
  const { canAssignToFleet } = useCollectorPermissions();
  const sendTelemetry = useSendCollectorsTelemetry();

  // Kept separate so the empty state can tell "there is nowhere else to move to" apart from
  // "you may not move it there" — they are different problems with different remedies.
  const otherFleets = (fleets ?? []).filter((fleet: Fleet) => fleet.id !== currentFleetId);
  const availableFleets = otherFleets.filter((fleet: Fleet) => canAssignToFleet(fleet.id));

  const fleetOptions = availableFleets.map((fleet: Fleet) => ({
    label: fleet.name,
    value: fleet.id,
  }));

  const handleSubmit = useCallback(
    async (values: FormValues) => {
      await reassignInstances({ instanceUids, fleetId: values.fleetId });

      if (origin === 'row') {
        sendTelemetry(TELEMETRY_EVENT_TYPE.COLLECTORS.INSTANCE.REASSIGNED, {
          app_action_value: 'instance-reassign',
          // The row path knows the instance UID and its fleet, but not the rest of the
          // shared instance payload -- it receives UIDs, not `CollectorInstanceView`s.
          instance_id: instanceUids[0],
          count: instanceUids.length,
          from_fleet_id: currentFleetId ?? null,
          to_fleet_id: values.fleetId,
        });
      } else {
        // The bulk selection carries UIDs only, so there is no source fleet to report --
        // the instances may well have come from several. `from_fleet_id` is omitted rather
        // than sent as a constant null.
        sendTelemetry(TELEMETRY_EVENT_TYPE.COLLECTORS.INSTANCE.BULK_REASSIGNED, {
          app_action_value: 'instance-bulk-reassign',
          count: instanceUids.length,
          to_fleet_id: values.fleetId,
        });
      }

      onSuccess();
      onClose();
    },
    [instanceUids, origin, reassignInstances, onSuccess, onClose, currentFleetId, sendTelemetry],
  );

  const instanceCount = instanceUids.length;
  const descriptor = instanceCount === 1 ? 'instance' : 'instances';

  return (
    <Modal onHide={onClose} show>
      <Formik<FormValues> initialValues={{ fleetId: '' }} onSubmit={handleSubmit} validate={validate}>
        {({ isSubmitting, isValidating, values, setFieldValue }) => (
          <Form>
            <Modal.Header showCloseButton>
              <Modal.Title>
                Reassign {instanceCount} {descriptor} to fleet
              </Modal.Title>
            </Modal.Header>
            <Modal.Body>
              {fleetsLoading ? (
                <Spinner />
              ) : (
                <>
                  {fleetOptions.length === 0 ? (
                    <p>
                      {otherFleets.length === 0
                        ? `There is no other fleet to assign ${instanceCount === 1 ? 'this Collector' : 'these Collectors'} to.`
                        : 'You do not have permission to assign Collectors to any other fleet.'}
                    </p>
                  ) : (
                    <Select
                      placeholder="Select a fleet..."
                      options={fleetOptions}
                      value={values.fleetId}
                      onChange={(value: string) => setFieldValue('fleetId', value)}
                      clearable={false}
                    />
                  )}
                </>
              )}
            </Modal.Body>
            <Modal.Footer>
              <ModalSubmit
                submitButtonText={`Reassign ${descriptor}`}
                submitLoadingText="Reassigning..."
                onCancel={onClose}
                disabledSubmit={!values.fleetId || isValidating}
                isSubmitting={isSubmitting}
              />
            </Modal.Footer>
          </Form>
        )}
      </Formik>
    </Modal>
  );
};

export default ReassignFleetModal;
