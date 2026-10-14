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
import { useTranslation } from 'react-i18next';

import { Alert, BootstrapModalWrapper, Modal } from 'components/bootstrap';
import ModalSubmit from 'components/common/ModalSubmit';
import i18n from 'i18n';

type ContentProps = {
  entityName: string;
  onCancel: () => void;
  submitError: string | null;
  children: React.ReactNode;
};

const CreateModalContent = ({ entityName, onCancel, submitError, children }: ContentProps) => {
  const { t } = useTranslation('common');
  const { isSubmitting, isValidating, isValid } = useFormikContext();

  return (
    <Form className="form form-horizontal">
      <Modal.Header>
        <Modal.Title>{t('create_entity.title', { entityName })}</Modal.Title>
      </Modal.Header>
      <Modal.Body>
        <div className="container-fluid">
          {children}
          {submitError && (
            <Alert bsStyle="danger" title={t('create_entity.save_failed')}>
              {submitError}
            </Alert>
          )}
        </div>
      </Modal.Body>
      <Modal.Footer>
        <ModalSubmit
          submitButtonText={t('create_entity.submit', { entityName })}
          submitLoadingText={t('create_entity.submitting', { entityName })}
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
      setSubmitError(error?.message ?? i18n.t('create_entity.unknown_error', { ns: 'common' }));
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
