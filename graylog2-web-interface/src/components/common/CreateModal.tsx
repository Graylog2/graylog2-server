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
import type { FormikErrors } from 'formik';
import { Formik, Form, useFormikContext } from 'formik';

import { Alert, BootstrapModalWrapper, Modal } from 'components/bootstrap';
import ModalSubmit from 'components/common/ModalSubmit';

type ContentProps = {
  entityName: string;
  onCancel: () => void;
  submitError: string | null;
  children: React.ReactNode;
};

const CreateModalContent = ({ entityName, onCancel, submitError, children }: ContentProps) => {
  const { isSubmitting, isValidating, isValid } = useFormikContext();
  const entityNameLower = entityName.toLowerCase();

  return (
    <Form className="form form-horizontal">
      <Modal.Header>
        <Modal.Title>Create {entityName}</Modal.Title>
      </Modal.Header>
      <Modal.Body>
        <div className="container-fluid">
          {children}
          {submitError && (
            <Alert bsStyle="danger" title="Failed to save">
              {submitError}
            </Alert>
          )}
        </div>
      </Modal.Body>
      <Modal.Footer>
        <ModalSubmit
          submitButtonText={`Create ${entityNameLower}`}
          submitLoadingText={`Creating ${entityNameLower}...`}
          isSubmitting={isSubmitting}
          isAsyncSubmit
          disabledSubmit={!isValid || isValidating}
          onCancel={onCancel}
        />
      </Modal.Footer>
    </Form>
  );
};

type Props<TValues extends object> = {
  entityName: string;
  show: boolean;
  onClose: () => void;
  initialValues: TValues;
  onSubmit: (values: TValues) => Promise<void>;
  validate?: (values: TValues) => FormikErrors<TValues> | Promise<FormikErrors<TValues>>;
  children: React.ReactNode;
};

const CreateModal = <TValues extends object>({
  entityName,
  show,
  onClose,
  initialValues,
  onSubmit,
  validate = undefined,
  children,
}: Props<TValues>) => {
  const [submitError, setSubmitError] = useState<string | null>(null);

  const handleSubmit = async (values: TValues) => {
    setSubmitError(null);

    try {
      await onSubmit(values);
      onClose();
    } catch (error) {
      setSubmitError(error?.message ?? 'An error occurred. Please try again.');
    }
  };

  return (
    <BootstrapModalWrapper showModal={show} onHide={onClose}>
      <Formik<TValues> initialValues={initialValues} onSubmit={handleSubmit} validate={validate} validateOnBlur={false}>
        <CreateModalContent entityName={entityName} onCancel={onClose} submitError={submitError}>
          {children}
        </CreateModalContent>
      </Formik>
    </BootstrapModalWrapper>
  );
};

export default CreateModal;
