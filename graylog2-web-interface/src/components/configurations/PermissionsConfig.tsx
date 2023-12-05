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
import { useEffect, useState } from 'react';
import styled, { css } from 'styled-components';
import { Form, Formik } from 'formik';

import { useStore } from 'stores/connect';
import type { Store } from 'stores/StoreTypes';
import type { PermissionsConfigType } from 'stores/configurations/ConfigurationsStore';
import { ConfigurationsActions, ConfigurationsStore } from 'stores/configurations/ConfigurationsStore';
import { ConfigurationType } from 'components/configurations/ConfigurationTypes';
import { getConfig } from 'components/configurations/helpers';
import { Button, Col, Modal, Row } from 'components/bootstrap';
import FormikInput from 'components/common/FormikInput';
import Spinner from 'components/common/Spinner';
import { InputDescription, ModalSubmit, IfPermitted } from 'components/common';
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

const PermissionsConfig = () => {
  const [showModal, setShowModal] = useState<boolean>(false);
  const [config, setConfig] = useState<PermissionsConfigType | undefined>(undefined);
  const configuration = useStore(ConfigurationsStore as Store<Record<string, any>>, (state) => state?.configuration);

  const sendTelemetry = useSendTelemetry();
  const { pathname } = useLocation();

  useEffect(() => {
    ConfigurationsActions.listPermissionsConfig(ConfigurationType.PERMISSIONS_CONFIG).then(() => {
      setConfig(getConfig(ConfigurationType.PERMISSIONS_CONFIG, configuration));
    });
  }, [configuration]);

  const saveConfig = (values: PermissionsConfigType) => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.CONFIGURATIONS.PERMISSIONS_UPDATED, {
      app_pathname: getPathnameWithoutId(pathname),
      app_section: 'permissions',
      app_action_value: 'configuration-save',
    });

    ConfigurationsActions.update(ConfigurationType.PERMISSIONS_CONFIG, values).then(() => {
      setShowModal(false);
    });
  };

  const resetConfig = () => {
    setShowModal(false);
  };

  const modalTitle = 'Configure Permissions';

  return (
    <div>
      <h2>Permissions Configuration</h2>
      <p>These settings can be used to control which entity sharing options are available.</p>

      {!config ? <Spinner /> : (
        <>
          <StyledDefList>
            <dt>Share with everyone:</dt>
            <dd>{config.allow_sharing_with_everyone ? 'Enabled' : 'Disabled'}</dd>
            <dt>Share with users:</dt>
            <dd>{config.allow_sharing_with_users ? 'Enabled' : 'Disabled'}</dd>
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
                 onHide={resetConfig}
                 aria-modal="true"
                 aria-labelledby="dialog_label">
            <Formik onSubmit={saveConfig} initialValues={config}>

              {({ isSubmitting }) => (
                <Form>
                  <Modal.Header closeButton>
                    <Modal.Title id="dialog_label">{modalTitle}</Modal.Title>
                  </Modal.Header>

                  <Modal.Body>
                    <div>
                      <Row>
                        <Col sm={12}>
                          <FormikInput type="checkbox"
                                       name="allow_sharing_with_everyone"
                                       id="shareWithEveryone"
                                       label={(
                                         <LabelSpan>Share with everyone</LabelSpan>
                                       )} />
                          <InputDescription help="Control whether it is possible to share with everyone." />
                        </Col>
                        <Col sm={12}>
                          <FormikInput type="checkbox"
                                       name="allow_sharing_with_users"
                                       id="shareWithUsers"
                                       label={(
                                         <LabelSpan>Share with users</LabelSpan>
                                       )} />
                          <InputDescription help="Control whether it is possible to share with single users." />
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

export default PermissionsConfig;
