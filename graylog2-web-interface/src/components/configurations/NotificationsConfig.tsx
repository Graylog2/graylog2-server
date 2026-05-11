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

import { Alert, Button, Col, Modal, Row } from 'components/bootstrap';
import FormikInput from 'components/common/FormikInput';
import Spinner from 'components/common/Spinner';
import { IfPermitted, InputDescription, ModalSubmit } from 'components/common';
import useCurrentUser from 'hooks/useCurrentUser';
import { isPermitted } from 'util/PermissionsMixin';
import useNotificationConfig from 'components/notifications/hooks/useNotificationConfig';
import type { SystemNotificationConfig } from 'components/notifications/types';

const DEFAULT_RETENTION_DAYS = 30;
const MIN_RETENTION_DAYS = 1;

const validate = (values: SystemNotificationConfig) => {
  const errors: Partial<Record<keyof SystemNotificationConfig, string>> = {};

  if (values.retention_days === undefined || values.retention_days === null) {
    errors.retention_days = 'Retention days is required.';
  } else if (!Number.isInteger(Number(values.retention_days))) {
    errors.retention_days = 'Retention days must be a whole number.';
  } else if (Number(values.retention_days) < MIN_RETENTION_DAYS) {
    errors.retention_days = `Retention days must be at least ${MIN_RETENTION_DAYS}.`;
  }

  return errors;
};

const NotificationsConfig = () => {
  const currentUser = useCurrentUser();
  const userPermissions = currentUser?.permissions ?? [];
  const canRead = isPermitted(userPermissions, ['notifications_config:read']);
  const canUpdate = isPermitted(userPermissions, ['notifications_config:update']);

  const { config, isLoading, update } = useNotificationConfig({
    readEnabled: canRead,
    updateEnabled: canUpdate,
  });

  const [showModal, setShowModal] = useState(false);

  if (!canRead) {
    return (
      <div>
        <h2>Notifications Configuration</h2>
        <Alert bsStyle="info">
          You don&apos;t have permission to view the notifications retention configuration.
        </Alert>
      </div>
    );
  }

  if (isLoading || !config) {
    return <Spinner />;
  }

  const initialValues: SystemNotificationConfig = {
    retention_days: config.retention_days ?? DEFAULT_RETENTION_DAYS,
  };

  const closeModal = () => setShowModal(false);

  const saveConfig = async (values: SystemNotificationConfig) => {
    await update({ retention_days: Number(values.retention_days) });
    closeModal();
  };

  return (
    <div>
      <h2>Notifications Configuration</h2>
      <p>How long to keep system notifications before they are automatically removed.</p>

      <dl className="deflist">
        <dt>Retention:</dt>
        <dd>{config.retention_days} {config.retention_days === 1 ? 'day' : 'days'}</dd>
      </dl>

      <IfPermitted permissions="notifications_config:update">
        <p>
          <Button type="button" bsSize="xs" bsStyle="info" onClick={() => setShowModal(true)}>
            Edit configuration
          </Button>
        </p>
      </IfPermitted>

      <Modal show={showModal} onHide={closeModal}>
        <Formik<SystemNotificationConfig>
          initialValues={initialValues}
          onSubmit={saveConfig}
          validate={validate}>
          {({ isSubmitting }) => (
            <Form>
              <Modal.Header>
                <Modal.Title>Configure Notifications Retention</Modal.Title>
              </Modal.Header>
              <Modal.Body>
                <Row>
                  <Col sm={12}>
                    <FormikInput
                      type="number"
                      name="retention_days"
                      id="retention_days"
                      label="Retention (days)"
                      min={MIN_RETENTION_DAYS}
                      required
                    />
                    <InputDescription
                      help={`Default: ${DEFAULT_RETENTION_DAYS} days. Notifications older than this are removed by the periodic cleanup job.`}
                    />
                  </Col>
                </Row>
              </Modal.Body>
              <Modal.Footer>
                <ModalSubmit
                  onCancel={closeModal}
                  isSubmitting={isSubmitting}
                  isAsyncSubmit
                  submitLoadingText="Saving…"
                  submitButtonText="Save"
                />
              </Modal.Footer>
            </Form>
          )}
        </Formik>
      </Modal>
    </div>
  );
};

export default NotificationsConfig;
