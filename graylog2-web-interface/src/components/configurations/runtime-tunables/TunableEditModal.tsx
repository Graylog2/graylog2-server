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
import { Form, Formik } from 'formik';
import type { FormikHelpers } from 'formik';

import { Alert, Button, Modal } from 'components/bootstrap';
import { ConfirmDialog, ModalSubmit } from 'components/common';
import UserNotification from 'util/UserNotification';

import ObjectFields from './fields/ObjectFields';
import type { RuntimeTunable, TunableValues } from './types';
import validateTunable from './validateTunable';
import { toFormErrors } from './serverErrors';
import { toFormValues } from './formValues';

type Props = {
  tunable: RuntimeTunable;
  onSave: (values: TunableValues) => Promise<unknown>;
  onReset: () => Promise<unknown>;
  onClose: () => void;
};

const TunableEditModal = ({ tunable, onSave, onReset, onClose }: Props) => {
  const [showResetConfirmation, setShowResetConfirmation] = useState(false);

  const submit = async (values: TunableValues, { setErrors, setStatus }: FormikHelpers<TunableValues>) => {
    setStatus(undefined);
    try {
      await onSave(values);
      onClose();
    } catch (error) {
      const formErrors = toFormErrors(error);
      if (!formErrors) {
        UserNotification.error(`Updating configuration failed: ${error}`, 'Could not update configuration');

        return;
      }
      setErrors(formErrors.fieldErrors);
      setStatus(formErrors.globalErrors);
    }
  };

  return (
    <Modal show onHide={onClose}>
      <Formik<TunableValues>
        initialValues={toFormValues(tunable.schema, tunable.value)}
        validate={(values) => validateTunable(tunable.schema, values)}
        onSubmit={submit}>
        {({ isSubmitting, status }) => (
          <Form>
            <Modal.Header>
              <Modal.Title>Update {tunable.title}</Modal.Title>
            </Modal.Header>
            <Modal.Body>
              {status?.length > 0 && <Alert bsStyle="danger">{status.join(' ')}</Alert>}
              <ObjectFields schema={tunable.schema} />
            </Modal.Body>
            <Modal.Footer>
              <ModalSubmit
                leftCol={
                  <Button bsStyle="warning" onClick={() => setShowResetConfirmation(true)}>
                    Reset to defaults
                  </Button>
                }
                onCancel={onClose}
                isSubmitting={isSubmitting}
                isAsyncSubmit
                submitLoadingText="Updating configuration..."
                submitButtonText="Update configuration"
              />
            </Modal.Footer>
          </Form>
        )}
      </Formik>
      {showResetConfirmation && (
        <ConfirmDialog
          show
          title={`Reset ${tunable.title}`}
          btnConfirmText="Reset"
          onCancel={() => setShowResetConfirmation(false)}
          onConfirm={() =>
            // the mutation already notifies about failures, the modal stays open for another attempt
            onReset().then(onClose, () => setShowResetConfirmation(false))
          }>
          All settings of {tunable.title} will be reset to their defaults.
        </ConfirmDialog>
      )}
    </Modal>
  );
};

export default TunableEditModal;
