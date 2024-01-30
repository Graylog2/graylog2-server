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
import { useState, useEffect } from 'react';
import styled, { css } from 'styled-components';
import { Form, Formik } from 'formik';
import type { UserConfigType } from 'src/stores/configurations/ConfigurationsStore';

import { useStore } from 'stores/connect';
import type { Store } from 'stores/StoreTypes';
import { ConfigurationsActions, ConfigurationsStore } from 'stores/configurations/ConfigurationsStore';
import { getConfig } from 'components/configurations/helpers';
import { ConfigurationType } from 'components/configurations/ConfigurationTypes';
import { Button, Col, Modal, Row } from 'components/bootstrap';
import FormikInput from 'components/common/FormikInput';
import Spinner from 'components/common/Spinner';
import { InputDescription, ModalSubmit, IfPermitted, ISODurationInput } from 'components/common';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import useLocation from 'routing/useLocation';
import { getPathnameWithoutId } from 'util/URLUtils';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

const StyledDefList = styled.dl.attrs({ className: 'deflist' })(({ theme }) => css`
  &&.deflist {
    dd {
      padding-left: ${theme.spacings.md};
      margin-left: 200px;
    }
  }
`);

const LabelSpan = styled.span(({ theme }) => css`
  margin-left: ${theme.spacings.sm};
  font-weight: bold;
`);

const UserConfig = () => {
  const [showModal, setShowModal] = useState<boolean>(false);
  const [viewConfig, setViewConfig] = useState<UserConfigType | undefined>(undefined);
  const [formConfig, setFormConfig] = useState<UserConfigType | undefined>(undefined);
  const configuration = useStore(ConfigurationsStore as Store<Record<string, any>>, (state) => state?.configuration);

  const sendTelemetry = useSendTelemetry();
  const { pathname } = useLocation();

  useEffect(() => {
    ConfigurationsActions.listUserConfig(ConfigurationType.USER_CONFIG).then(() => {
      const config = getConfig(ConfigurationType.USER_CONFIG, configuration);

      setViewConfig(config);
      setFormConfig(config);
    });
  }, [configuration]);

  const saveConfig = (values) => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.CONFIGURATIONS.USER_UPDATED, {
      app_pathname: getPathnameWithoutId(pathname),
      app_section: 'user',
      app_action_value: 'configuration-save',
    });

    ConfigurationsActions.update(ConfigurationType.USER_CONFIG, values).then(() => {
      setShowModal(false);
    });
  };

  const resetConfig = () => {
    setShowModal(false);
    setFormConfig(viewConfig);
  };

  const timeoutIntervalValidator = (milliseconds: number) => milliseconds >= 1000;

  const modalTitle = 'Update User Configuration';

  return (
    <div>
      <h2>Users Configuration</h2>
      <p>These settings can be used to set a global session timeout value.</p>

      {!viewConfig ? <Spinner /> : (
        <>
          <StyledDefList>
            <dt>Global session timeout:</dt>
            <dd>{viewConfig.enable_global_session_timeout ? 'Enabled' : 'Disabled'}</dd>
            <dt>Timeout interval:</dt>
            <dd>{viewConfig.enable_global_session_timeout ? viewConfig.global_session_timeout_interval : '-'}</dd>
          </StyledDefList>

          <IfPermitted permissions="clusterconfigentry:edit">
            <p>
              <Button type="button"
                      bsSize="xs"
                      bsStyle="info"
                      onClick={() => {
                        setShowModal(true);
                      }}>
                Edit configuration
              </Button>
            </p>
          </IfPermitted>

          <Modal show={showModal && !!formConfig}
                 onHide={resetConfig}
                 aria-modal="true"
                 aria-labelledby="dialog_label">
            <Formik onSubmit={saveConfig} initialValues={formConfig}>
              {({ isSubmitting, values, setFieldValue }) => (
                <Form>
                  <Modal.Header closeButton>
                    <Modal.Title id="dialog_label">{modalTitle}</Modal.Title>
                  </Modal.Header>

                  <Modal.Body>
                    <div>
                      <Row>
                        <Col sm={12}>
                          <FormikInput type="checkbox"
                                       name="enable_global_session_timeout"
                                       id="enable_global_session_timeout"
                                       label={(
                                         <LabelSpan>Enable global session timeout</LabelSpan>
                                       )} />
                          <InputDescription help="If enabled, it will be set for all the users." />
                        </Col>
                        <Col sm={12}>
                          <fieldset>
                            <ISODurationInput id="global_session_timeout_interval"
                                              duration={values.global_session_timeout_interval}
                                              update={(value) => setFieldValue('global_session_timeout_interval', value)}
                                              label="Global session timeout interval (as ISO8601 Duration)"
                                              help="Session automatically end after this amount of time, unless they are actively used."
                                              validator={timeoutIntervalValidator}
                                              errorText="invalid (min: 1 second)"
                                              disabled={!values.enable_global_session_timeout}
                                              required />
                          </fieldset>
                        </Col>
                      </Row>
                    </div>
                  </Modal.Body>

                  <Modal.Footer>
                    <ModalSubmit onCancel={resetConfig}
                                 isSubmitting={isSubmitting}
                                 isAsyncSubmit
                                 submitLoadingText="Update configuration"
                                 submitButtonText="Update configuration" />
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

export default UserConfig;
