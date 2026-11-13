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

import { Button, Col, Modal, Row } from 'components/bootstrap';
import FormikInput from 'components/common/FormikInput';
import Spinner from 'components/common/Spinner';
import { ModalSubmit, IfPermitted } from 'components/common';
import { ConfigurationType } from 'components/configurations/ConfigurationTypes';
import useClusterConfig, { useUpdateClusterConfig } from 'hooks/useClusterConfig';
import NumberUtils from 'util/NumberUtils';
import UserNotification from 'util/UserNotification';

import type { BeatsInputDefaultsConfigType } from './beatsInputDefaults';
import { DEFAULT_BEATS_INPUT_DEFAULTS_CONFIG, validateBeatsInputDefaults } from './beatsInputDefaults';

const configType = ConfigurationType.BEATS_INPUT_DEFAULTS_CONFIG;

const formatSize = (bytes: number) => `${NumberUtils.formatBytes(bytes)} (${bytes} bytes)`;

const BeatsInputsConfig = () => {
  const [showModal, setShowModal] = useState(false);
  const { data, isLoading } = useClusterConfig<BeatsInputDefaultsConfigType | null>(configType);
  const { mutateAsync: updateConfig } = useUpdateClusterConfig<BeatsInputDefaultsConfigType>(configType);

  const config = data ?? DEFAULT_BEATS_INPUT_DEFAULTS_CONFIG;

  const saveConfig = (values: BeatsInputDefaultsConfigType) =>
    updateConfig({ max_event_size: Number(values.max_event_size) }).then(
      () => {
        UserNotification.success('Configuration updated successfully');
        setShowModal(false);
      },
      (error) => {
        UserNotification.error(`Updating Beats input defaults failed: ${error}`, 'Could not update configuration');
      },
    );

  const closeModal = () => setShowModal(false);

  return (
    <div>
      <h2>Beats Inputs</h2>
      <p>
        Defaults for all Beats inputs that don&apos;t override them, i.e. whose max. event size is set to 0. Inputs
        pick up changes when they are restarted.
      </p>

      {isLoading ? (
        <Spinner />
      ) : (
        <>
          <dl className="deflist">
            <dt>Max. event size:</dt>
            <dd>{formatSize(config.max_event_size)}</dd>
          </dl>

          <IfPermitted permissions="clusterconfigentry:edit">
            <Button type="button" bsSize="xs" bsStyle="info" onClick={() => setShowModal(true)}>
              Edit configuration
            </Button>
          </IfPermitted>

          <Modal show={showModal} onHide={closeModal}>
            <Formik onSubmit={saveConfig} initialValues={config} validate={validateBeatsInputDefaults}>
              {({ isSubmitting, values }) => (
                <Form>
                  <Modal.Header>
                    <Modal.Title>Update Beats Input Defaults</Modal.Title>
                  </Modal.Header>

                  <Modal.Body>
                    <Row>
                      <Col sm={12}>
                        <FormikInput
                          type="number"
                          name="max_event_size"
                          id="max_event_size"
                          label="Max. event size (bytes)"
                          min={1}
                          required
                          autoFocus
                          help={`Larger events are rejected and the connection to the sender is closed. Current value: ${formatSize(Number(values.max_event_size))}.`}
                        />
                      </Col>
                    </Row>
                  </Modal.Body>

                  <Modal.Footer>
                    <ModalSubmit
                      onCancel={closeModal}
                      isSubmitting={isSubmitting}
                      isAsyncSubmit
                      submitLoadingText="Update configuration"
                      submitButtonText="Update configuration"
                    />
                  </Modal.Footer>
                </Form>
              )}
            </Formik>
          </Modal>
        </>
      )}
    </div>
  );
};

export default BeatsInputsConfig;
