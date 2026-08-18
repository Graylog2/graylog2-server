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

import { Button, Col, Modal, Row } from 'components/bootstrap';
import FormikInput from 'components/common/FormikInput';
import Spinner from 'components/common/Spinner';
import { ModalSubmit, IfPermitted } from 'components/common';
import { ConfigurationsActions, ConfigurationsStore } from 'stores/configurations/ConfigurationsStore';
import { ConfigurationType } from 'components/configurations/ConfigurationTypes';
import { getConfig } from 'components/configurations/helpers';
import { useStore } from 'stores/connect';
import type { Store } from 'stores/StoreTypes';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import useLocation from 'routing/useLocation';
import { getPathnameWithoutId } from 'util/URLUtils';
import type { WelcomePageConfigType } from 'components/common/types';

const StyledDefList = styled.dl.attrs({ className: 'deflist' })(
  ({ theme }) => css`
    &&.deflist {
      dd {
        padding-left: ${theme.spacings.md};
        margin-left: 400px;
      }
    }
  `,
);

const LabelSpan = styled.span(
  ({ theme }) => css`
    margin-left: ${theme.spacings.sm};
    font-weight: bold;
  `,
);

const configType = ConfigurationType.WELCOME_PAGE_CONFIG;
const DEFAULT_CONFIG: WelcomePageConfigType = { disable_queries: false };

type FormValues = { queries_enabled: boolean };

const WelcomePageConfig = () => {
  const [showModal, setShowModal] = useState<boolean>(false);
  const [viewConfig, setViewConfig] = useState<WelcomePageConfigType | undefined>(undefined);
  const [formConfig, setFormConfig] = useState<FormValues | undefined>(undefined);
  const configuration = useStore(ConfigurationsStore as Store<Record<string, any>>, (state) => state?.configuration);

  const sendTelemetry = useSendTelemetry();
  const { pathname } = useLocation();

  useEffect(() => {
    ConfigurationsActions.list(configType).then(() => {
      const config = getConfig(configType, configuration) ?? DEFAULT_CONFIG;

      setViewConfig(config);
      setFormConfig({ queries_enabled: !config.disable_queries });
    });
  }, [configuration]);

  const saveConfig = (values: FormValues) => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.CONFIGURATIONS.WELCOME_PAGE_UPDATED, {
      app_pathname: getPathnameWithoutId(pathname),
      app_section: 'welcome-page',
      app_action_value: 'configuration-save',
    });

    ConfigurationsActions.update(configType, { disable_queries: !values.queries_enabled }).then(() => {
      setShowModal(false);
    });
  };

  const resetConfig = () => {
    setShowModal(false);
    setFormConfig({ queries_enabled: !viewConfig.disable_queries });
  };

  return (
    <div>
      <h2>Welcome Page</h2>
      <p>Configure whether the metrics widgets on the welcome page run their underlying queries.</p>

      {!viewConfig ? (
        <Spinner />
      ) : (
        <>
          <StyledDefList>
            <dt>Welcome page queries:</dt>
            <dd>{viewConfig.disable_queries ? 'Disabled' : 'Enabled'}</dd>
          </StyledDefList>

          <IfPermitted permissions="clusterconfigentry:edit">
            <p>
              <Button
                type="button"
                bsSize="xs"
                bsStyle="info"
                onClick={() => {
                  setShowModal(true);
                }}>
                Edit configuration
              </Button>
            </p>
          </IfPermitted>

          <Modal show={showModal && !!formConfig} onHide={resetConfig}>
            <Formik onSubmit={saveConfig} initialValues={formConfig}>
              {({ isSubmitting }) => (
                <Form>
                  <Modal.Header>
                    <Modal.Title>Update Welcome Page Configuration</Modal.Title>
                  </Modal.Header>

                  <Modal.Body>
                    <Row>
                      <Col sm={12}>
                        <FormikInput
                          type="checkbox"
                          name="queries_enabled"
                          id="queries_enabled"
                          label={<LabelSpan>Enable welcome page queries</LabelSpan>}
                          help="If disabled, the metrics widgets on the welcome page will not run their underlying queries. Disable this if you are concerned about the performance impact of these queries."
                        />
                      </Col>
                    </Row>
                  </Modal.Body>

                  <Modal.Footer>
                    <ModalSubmit
                      onCancel={resetConfig}
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

export default WelcomePageConfig;
