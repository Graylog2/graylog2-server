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
import { useTranslation } from 'react-i18next';

import { Button, Col, Modal, Row } from 'components/bootstrap';
import FormikInput from 'components/common/FormikInput';
import Spinner from 'components/common/Spinner';
import { ModalSubmit, IfPermitted } from 'components/common';
import { ConfigurationsActions, ConfigurationsStore } from 'stores/configurations/ConfigurationsStore';
import { ConfigurationType } from 'components/configurations/ConfigurationTypes';
import { getConfig } from 'components/configurations/helpers';
import { useStore } from 'stores/connect';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import type { WelcomePageConfigType } from 'components/common/types';
import { DEFAULT_WELCOME_PAGE_CONFIG } from 'logic/welcome/welcomePageConfig';

const StyledDefList = styled.dl.attrs({ className: 'deflist' })(
  ({ theme }) => css`
    &&.deflist {
      dd {
        padding-left: ${theme.spacings.md};
        margin-left: 200px;
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

type FormValues = { metrics_enabled: boolean };

const WelcomePageConfig = () => {
  const { t } = useTranslation('configurations');
  const [showModal, setShowModal] = useState<boolean>(false);
  const [viewConfig, setViewConfig] = useState<WelcomePageConfigType | undefined>(undefined);
  const [formConfig, setFormConfig] = useState<FormValues | undefined>(undefined);
  const configuration = useStore(ConfigurationsStore, (state) => state?.configuration);

  const sendTelemetry = useSendTelemetry('welcome-page');

  useEffect(() => {
    ConfigurationsActions.list(configType).then(() => {
      const config = getConfig(configType, configuration) ?? DEFAULT_WELCOME_PAGE_CONFIG;

      setViewConfig(config);
      setFormConfig({ metrics_enabled: !config.disable_metrics });
    });
  }, [configuration]);

  const saveConfig = (values: FormValues) => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.CONFIGURATIONS.WELCOME_PAGE_UPDATED, {
      app_action_value: 'configuration-save',
    });

    ConfigurationsActions.update(configType, { disable_metrics: !values.metrics_enabled }).then(() => {
      setShowModal(false);
    });
  };

  const resetConfig = () => {
    setShowModal(false);
    setFormConfig({ metrics_enabled: !viewConfig.disable_metrics });
  };

  return (
    <div>
      <h2>{t('welcome_page.title')}</h2>
      <p>{t('welcome_page.description')}</p>

      {!viewConfig ? (
        <Spinner />
      ) : (
        <>
          <StyledDefList>
            <dt>{t('welcome_page.metrics')}:</dt>
            <dd>{viewConfig.disable_metrics ? t('markdown.disabled') : t('markdown.enabled')}</dd>
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
                {t('shared.edit_configuration')}
              </Button>
            </p>
          </IfPermitted>

          <Modal show={showModal && !!formConfig} onHide={resetConfig}>
            <Formik onSubmit={saveConfig} initialValues={formConfig}>
              {({ isSubmitting }) => (
                <Form>
                  <Modal.Header>
                    <Modal.Title>{t('welcome_page.update_title')}</Modal.Title>
                  </Modal.Header>

                  <Modal.Body>
                    <Row>
                      <Col sm={12}>
                        <FormikInput
                          type="checkbox"
                          name="metrics_enabled"
                          id="metrics_enabled"
                          label={<LabelSpan>{t('welcome_page.enable_metrics')}</LabelSpan>}
                          help={t('welcome_page.enable_metrics_help')}
                        />
                      </Col>
                    </Row>
                  </Modal.Body>

                  <Modal.Footer>
                    <ModalSubmit
                      onCancel={resetConfig}
                      isSubmitting={isSubmitting}
                      isAsyncSubmit
                      submitLoadingText={t('shared.update_configuration')}
                      submitButtonText={t('shared.update_configuration')}
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
