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
import { useTranslation } from 'react-i18next';

import type { UserConfigType } from 'stores/configurations/ConfigurationsStore';
import { ConfigurationsActions, ConfigurationsStore } from 'stores/configurations/ConfigurationsStore';
import { useStore } from 'stores/connect';
import { getConfig } from 'components/configurations/helpers';
import { ConfigurationType } from 'components/configurations/ConfigurationTypes';
import { Button, Col, Modal, Row } from 'components/bootstrap';
import FormikInput from 'components/common/FormikInput';
import Spinner from 'components/common/Spinner';
import { InputDescription, ModalSubmit, IfPermitted, ISODurationInput } from 'components/common';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

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

const UserConfig = () => {
  const { t } = useTranslation('configurations');
  const [showModal, setShowModal] = useState<boolean>(false);
  const [viewConfig, setViewConfig] = useState<UserConfigType | undefined>(undefined);
  const [formConfig, setFormConfig] = useState<UserConfigType | undefined>(undefined);
  const configuration = useStore(ConfigurationsStore, (state) => state?.configuration);

  const sendTelemetry = useSendTelemetry('user');

  useEffect(() => {
    ConfigurationsActions.listUserConfig(ConfigurationType.USER_CONFIG).then(() => {
      const config = getConfig(ConfigurationType.USER_CONFIG, configuration);

      setViewConfig(config);
      setFormConfig({ ...config, restrict_access_token_to_admins: !config?.restrict_access_token_to_admins });
    });
  }, [configuration]);

  const saveConfig = (values: UserConfigType) => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.CONFIGURATIONS.USER_UPDATED, {
      app_action_value: 'configuration-save',
    });
    const newConfig = { ...values, restrict_access_token_to_admins: !values?.restrict_access_token_to_admins };

    ConfigurationsActions.update(ConfigurationType.USER_CONFIG, newConfig).then(() => {
      setShowModal(false);
    });
  };

  const resetConfig = () => {
    setShowModal(false);
    setFormConfig(viewConfig);
  };

  const timeoutIntervalValidator = (milliseconds: number) => milliseconds >= 1000;
  const defaultTokenTtlValidator = (milliseconds: number) => milliseconds >= 86400000;

  return (
    <div>
      <h2>{t('user.title')}</h2>
      <p>{t('user.description')}</p>

      {!viewConfig ? (
        <Spinner />
      ) : (
        <>
          <StyledDefList>
            <dt>{t('user.global_session_timeout')}:</dt>
            <dd>{viewConfig.enable_global_session_timeout ? t('markdown.enabled') : t('markdown.disabled')}</dd>
            <dt>{t('user.timeout_interval')}:</dt>
            <dd>{viewConfig.enable_global_session_timeout ? viewConfig.global_session_timeout_interval : '-'}</dd>
            <dt>{t('user.allow_personal_tokens')}:&nbsp;</dt>
            <dd>{!viewConfig.restrict_access_token_to_admins ? t('markdown.enabled') : t('markdown.disabled')}</dd>
            <dt>{t('user.allow_external_tokens')}:&nbsp;</dt>
            <dd>{viewConfig.allow_access_token_for_external_user ? t('markdown.enabled') : t('markdown.disabled')}</dd>
            <dt>{t('user.default_token_ttl')}:</dt>
            <dd>{viewConfig.default_ttl_for_new_tokens ? viewConfig.default_ttl_for_new_tokens : '-'}</dd>
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
              {({ isSubmitting, values, setFieldValue }) => (
                <Form>
                  <Modal.Header>
                    <Modal.Title>{t('user.update_title')}</Modal.Title>
                  </Modal.Header>

                  <Modal.Body>
                    <div>
                      <Row>
                        <Col sm={12}>
                          <FormikInput
                            type="checkbox"
                            name="enable_global_session_timeout"
                            id="enable_global_session_timeout"
                            label={<LabelSpan>{t('user.enable_global_session_timeout')}</LabelSpan>}
                          />
                          <InputDescription help={t('user.enable_global_session_timeout_help')} />
                        </Col>
                        <Col sm={12}>
                          <fieldset>
                            <ISODurationInput
                              id="global_session_timeout_interval"
                              duration={values.global_session_timeout_interval}
                              update={(value) => setFieldValue('global_session_timeout_interval', value)}
                              label={t('user.session_timeout_interval_label')}
                              help={t('user.session_timeout_interval_help')}
                              validator={timeoutIntervalValidator}
                              errorText={t('sidecar.inactive_threshold_error')}
                              disabled={!values.enable_global_session_timeout}
                              required
                            />
                          </fieldset>
                        </Col>
                        <Col sm={12}>
                          <FormikInput
                            type="checkbox"
                            name="restrict_access_token_to_admins"
                            id="restrict_access_token_to_admins"
                            label={<LabelSpan>{t('user.allow_personal_tokens')}</LabelSpan>}
                          />
                          <InputDescription help={t('user.allow_personal_tokens_help')} />
                        </Col>
                        <Col sm={12}>
                          <FormikInput
                            type="checkbox"
                            name="allow_access_token_for_external_user"
                            id="allow_access_token_for_external_user"
                            label={<LabelSpan>{t('user.allow_external_tokens')}</LabelSpan>}
                          />
                          <InputDescription help={t('user.allow_external_tokens_help')} />
                        </Col>
                        <Col sm={12}>
                          <fieldset>
                            <ISODurationInput
                              id="default_ttl_for_new_tokens"
                              duration={values.default_ttl_for_new_tokens}
                              update={(value) => setFieldValue('default_ttl_for_new_tokens', value)}
                              label={t('user.default_token_ttl_label')}
                              help={t('user.default_token_ttl_help')}
                              validator={defaultTokenTtlValidator}
                              errorText={t('user.default_token_ttl_error')}
                              disabled={!values.default_ttl_for_new_tokens}
                              required
                            />
                          </fieldset>
                        </Col>
                      </Row>
                    </div>
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

export default UserConfig;
