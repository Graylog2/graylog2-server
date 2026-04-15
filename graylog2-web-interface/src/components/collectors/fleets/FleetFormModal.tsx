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
import { useCallback, useRef } from 'react';
import { Formik, Form } from 'formik';
import type { FormikTouched } from 'formik';

import { Modal } from 'components/bootstrap';
import { FormikInput } from 'components/common';
import ModalSubmit from 'components/common/ModalSubmit';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

import useSendCollectorsTelemetry from '../hooks/useSendCollectorsTelemetry';
import type { Fleet } from '../types';

type FormValues = {
  name: string;
  description: string;
};

type Props = {
  fleet?: Fleet;
  onClose: () => void;
  onSave: (fleet: Omit<Fleet, 'id' | 'created_at' | 'updated_at'>) => Promise<{ id?: string } | void>;
};

const validate = (values: FormValues) => {
  const errors: Partial<Record<keyof FormValues, string>> = {};

  if (!values.name) {
    errors.name = 'Name is required';
  }

  return errors;
};

const FleetFormModal = ({ fleet = undefined, onClose, onSave }: Props) => {
  const isEdit = !!fleet;
  const sendTelemetry = useSendCollectorsTelemetry();

  const initialValues: FormValues = {
    name: fleet?.name || '',
    description: fleet?.description || '',
  };

  // Ref kept up-to-date from the Formik render prop so handleClose (which lives
  // outside Formik) can report abandonment context on cancel.
  const formStateRef = useRef<{ dirty: boolean; touched: FormikTouched<FormValues> }>({
    dirty: false,
    touched: {},
  });

  const handleClose = useCallback(() => {
    if (!isEdit) {
      const { dirty, touched } = formStateRef.current;
      const fields_touched = Object.keys(touched).filter((k) => Boolean((touched as Record<string, unknown>)[k]));

      sendTelemetry(TELEMETRY_EVENT_TYPE.COLLECTORS.FLEET.CREATE_CANCELLED, {
        app_action_value: 'fleet-create-cancel',
        dirty,
        fields_touched,
      });
    }
    onClose();
  }, [isEdit, onClose, sendTelemetry]);

  const handleSubmit = useCallback(
    (values: FormValues) =>
      onSave({
        name: values.name,
        description: values.description,
      }).then((saved) => {
        if (!isEdit) {
          const createdId = (saved && 'id' in saved) ? saved.id ?? '' : '';
          sendTelemetry(TELEMETRY_EVENT_TYPE.COLLECTORS.FLEET.CREATED, {
            app_action_value: 'fleet-create-submit',
            fleet_id: createdId,
          });
        }
        onClose();
      }),
    [isEdit, onSave, onClose, sendTelemetry],
  );

  return (
    <Modal show onHide={handleClose}>
      <Formik<FormValues> initialValues={initialValues} onSubmit={handleSubmit} validate={validate} validateOnMount>
        {({ isSubmitting, isValid, isValidating, dirty, touched }) => {
          formStateRef.current = { dirty, touched };

          return (
          <Form>
            <Modal.Header>
              <Modal.Title>{isEdit ? 'Edit Fleet' : 'New Fleet'}</Modal.Title>
            </Modal.Header>
            <Modal.Body>
              <FormikInput id="fleet-name" label="Name" name="name" help="A unique name for this fleet" required />
              <FormikInput
                id="fleet-description"
                label="Description"
                name="description"
                type="textarea"
                help="Optional description of this fleet's purpose"
              />
            </Modal.Body>
            <Modal.Footer>
              <ModalSubmit
                submitButtonText={isEdit ? 'Update fleet' : 'Create fleet'}
                submitLoadingText={isEdit ? 'Updating...' : 'Creating...'}
                onCancel={handleClose}
                disabledSubmit={isValidating || !isValid}
                isSubmitting={isSubmitting}
                isAsyncSubmit
              />
            </Modal.Footer>
          </Form>
          );
        }}
      </Formik>
    </Modal>
  );
};

export default FleetFormModal;
