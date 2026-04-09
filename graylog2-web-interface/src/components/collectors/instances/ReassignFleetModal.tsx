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

import { useFleets, useCollectorsMutations } from '../hooks';
import type { Fleet } from '../types';

type Props = {
  instanceUids: string[];
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

const ReassignFleetModal = ({ instanceUids, currentFleetId, onClose, onSuccess }: Props) => {
  const { data: fleets, isLoading: fleetsLoading } = useFleets();
  const { reassignInstances } = useCollectorsMutations();

  const availableFleets = (fleets || []).filter((fleet: Fleet) => fleet.id !== currentFleetId);

  const fleetOptions = availableFleets.map((fleet: Fleet) => ({
    label: fleet.name,
    value: fleet.id,
  }));

  const handleSubmit = useCallback(
    async (values: FormValues) => {
      await reassignInstances({ instanceUids, fleetId: values.fleetId });
      onSuccess?.();
      onClose();
    },
    [instanceUids, reassignInstances, onSuccess, onClose],
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
                <Select
                  placeholder="Select a fleet..."
                  options={fleetOptions}
                  value={values.fleetId}
                  onChange={(value: string) => setFieldValue('fleetId', value)}
                  clearable={false}
                />
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
