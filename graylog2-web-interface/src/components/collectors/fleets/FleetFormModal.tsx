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
import { FormikInput } from 'components/common';
import ModalSubmit from 'components/common/ModalSubmit';

import type { Fleet } from '../types';

type FormValues = {
  name: string;
  description: string;
  target_version: string;
};

type Props = {
  fleet?: Fleet;
  onClose: () => void;
  onSave: (fleet: Omit<Fleet, 'id' | 'created_at' | 'updated_at'>) => Promise<void>;
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

  const initialValues: FormValues = {
    name: fleet?.name || '',
    description: fleet?.description || '',
    target_version: fleet?.target_version || '',
  };

  const handleSubmit = useCallback(
    (values: FormValues) =>
      onSave({
        name: values.name,
        description: values.description,
        target_version: values.target_version || null,
      }).then(() => onClose()),
    [onSave, onClose],
  );

  return (
    <Modal show onHide={onClose}>
      <Formik<FormValues> initialValues={initialValues} onSubmit={handleSubmit} validate={validate}>
        {({ isSubmitting, isValidating }) => (
          <Form>
            <Modal.Header>
              <Modal.Title>{isEdit ? 'Edit Fleet' : 'New Fleet'}</Modal.Title>
            </Modal.Header>
            <Modal.Body>
              <FormikInput
                id="fleet-name"
                label="Name"
                name="name"
                help="A unique name for this fleet"
                required
              />
              <FormikInput
                id="fleet-description"
                label="Description"
                name="description"
                type="textarea"
                help="Optional description of this fleet's purpose"
              />
              <FormikInput
                id="fleet-target-version"
                label="Target Version"
                name="target_version"
                help="Optional collector version for this fleet"
                placeholder="e.g., 1.2.0"
              />
            </Modal.Body>
            <Modal.Footer>
              <ModalSubmit
                submitButtonText={isEdit ? 'Update fleet' : 'Create fleet'}
                submitLoadingText={isEdit ? 'Updating...' : 'Creating...'}
                onCancel={onClose}
                disabledSubmit={isValidating}
                isSubmitting={isSubmitting}
              />
            </Modal.Footer>
          </Form>
        )}
      </Formik>
    </Modal>
  );
};

export default FleetFormModal;
