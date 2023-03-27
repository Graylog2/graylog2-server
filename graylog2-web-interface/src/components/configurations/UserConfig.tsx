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
import type { DefaultTheme } from 'styled-components';
import styled, { css } from 'styled-components';
import { Form, Formik } from 'formik';
import type { UserConfigType } from 'src/stores/configurations/ConfigurationsStore';

import { Button, Col, Modal, Row } from 'components/bootstrap';
import FormikInput from 'components/common/FormikInput';
import Spinner from 'components/common/Spinner';
import { InputDescription, ModalSubmit, IfPermitted, ISODurationInput } from 'components/common';

type Props = {
  config: UserConfigType,
  updateConfig: (config: UserConfigType) => Promise<void>,
};

const StyledDefList = styled.dl.attrs({
  className: 'deflist',
})(({ theme }: { theme: DefaultTheme }) => css`
  &&.deflist {
    dd {
      padding-left: ${theme.spacings.md};
      margin-left: 200px;
    }
  }
`);

const LabelSpan = styled.span(({ theme }: { theme: DefaultTheme }) => css`
  margin-left: ${theme.spacings.sm};
  font-weight: bold;
`);

const UserConfig = ({ config, updateConfig }: Props) => {
  const [showModal, setShowModal] = useState<boolean>(false);

  const _saveConfig = (values) => {
    updateConfig(values).then(() => {
      setShowModal(false);
    });
  };

  const _resetConfig = () => {
    setShowModal(false);
  };

  const _timeoutIntervalValidator = (milliseconds: number) => {
    return milliseconds >= 1000;
  };

  const modalTitle = 'Update User Configuration';

  return (
    <div>
      <h2>User Configuration</h2>
      <p>These settings can be used to set a global session timeout value.</p>

      {!config ? <Spinner /> : (
        <>
          <StyledDefList>
            <dt>Global session timeout:</dt>
            <dd>{config.enable_global_session_timeout ? 'Enabled' : 'Disabled'}</dd>
            <dt>Timeout interval:</dt>
            <dd>{config.enable_global_session_timeout ? config.global_session_timeout_interval : '-'}</dd>
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

          <Modal show={showModal}
                 onHide={_resetConfig}
                 aria-modal="true"
                 aria-labelledby="dialog_label"
                 data-app-section="configurations_user"
                 data-event-element={modalTitle}>
            <Formik onSubmit={_saveConfig} initialValues={config}>

              {({ isSubmitting, values, setFieldValue }) => {
                return (
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
                                                validator={_timeoutIntervalValidator}
                                                errorText="invalid (min: 1 second)"
                                                disabled={!values.enable_global_session_timeout}
                                                required />
                            </fieldset>
                          </Col>
                        </Row>
                      </div>
                    </Modal.Body>

                    <Modal.Footer>
                      <ModalSubmit onCancel={_resetConfig}
                                   isSubmitting={isSubmitting}
                                   isAsyncSubmit
                                   submitLoadingText="Update configuration"
                                   submitButtonText="Update configuration" />
                    </Modal.Footer>
                  </Form>
                );
              }}

            </Formik>
          </Modal>
        </>
      )}
    </div>
  );
};

export default UserConfig;
