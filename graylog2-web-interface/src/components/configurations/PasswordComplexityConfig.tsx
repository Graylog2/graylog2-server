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
import type { PasswordComplexityConfigType } from 'stores/configurations/ConfigurationsStore';
import { ConfigurationsActions, ConfigurationsStore } from 'stores/configurations/ConfigurationsStore';
import { ConfigurationType } from 'components/configurations/ConfigurationTypes';
import { getConfig } from 'components/configurations/helpers';
import { useStore } from 'stores/connect';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import { DEFAULT_PASSWORD_COMPLEXITY_CONFIG, PASSWORD_SPECIAL_CHARACTERS } from 'logic/users/passwordComplexity';

const StyledDefList = styled.dl.attrs({ className: 'deflist' })(
  ({ theme }) => css`
    &&.deflist {
      dd {
        padding-left: ${theme.spacings.md};
        margin-left: 300px;
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

const configType = ConfigurationType.PASSWORD_COMPLEXITY_CONFIG;

const PasswordComplexityConfig = () => {
  const { t } = useTranslation('configurations');
  const [showModal, setShowModal] = useState<boolean>(false);
  const [viewConfig, setViewConfig] = useState<PasswordComplexityConfigType | undefined>(undefined);
  const [formConfig, setFormConfig] = useState<PasswordComplexityConfigType | undefined>(undefined);
  const configuration = useStore(ConfigurationsStore, (state) => state?.configuration);

  const sendTelemetry = useSendTelemetry('password-complexity');

  useEffect(() => {
    ConfigurationsActions.listPasswordComplexityConfig(configType).then(() => {
      const config = getConfig(configType, configuration) ?? DEFAULT_PASSWORD_COMPLEXITY_CONFIG;

      setViewConfig(config);
      setFormConfig(config);
    });
  }, [configuration]);

  const saveConfig = (values: PasswordComplexityConfigType) => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.CONFIGURATIONS.PASSWORD_COMPLEXITY_UPDATED, {
      app_action_value: 'configuration-save',
    });

    const payload = { ...values, min_length: Number(values.min_length) };

    ConfigurationsActions.update(configType, payload).then(() => {
      setShowModal(false);
    });
  };

  const resetConfig = () => {
    setShowModal(false);
    setFormConfig(viewConfig);
  };

  const validate = (
    values: PasswordComplexityConfigType,
  ): Partial<Record<keyof PasswordComplexityConfigType, string>> => {
    const errors: Partial<Record<keyof PasswordComplexityConfigType, string>> = {};

    if (!values.min_length || Number(values.min_length) < 1) {
      errors.min_length = t('password_complexity.min_length_error');
    }

    return errors;
  };

  const renderRequirement = (label: string, value: React.ReactNode) => (
    <>
      <dt>{label}</dt>
      <dd aria-label={label}>{value}</dd>
    </>
  );

  const requirementLabel = (required: boolean) =>
    required ? t('password_complexity.required') : t('password_complexity.not_required');

  return (
    <div>
      <h2>{t('password_complexity.title')}</h2>
      <p>{t('password_complexity.description')}</p>

      {!viewConfig ? (
        <Spinner />
      ) : (
        <>
          <StyledDefList>
            {renderRequirement(`${t('password_complexity.min_length')}:`, viewConfig.min_length)}
            {renderRequirement(
              `${t('password_complexity.require_uppercase')}:`,
              requirementLabel(viewConfig.require_uppercase),
            )}
            {renderRequirement(
              `${t('password_complexity.require_lowercase')}:`,
              requirementLabel(viewConfig.require_lowercase),
            )}
            {renderRequirement(
              `${t('password_complexity.require_numbers')}:`,
              requirementLabel(viewConfig.require_numbers),
            )}
            {renderRequirement(
              `${t('password_complexity.require_special_chars')}:`,
              requirementLabel(viewConfig.require_special_chars),
            )}
          </StyledDefList>

          <IfPermitted permissions="clusterconfigentry:edit">
            <Button
              type="button"
              bsSize="xs"
              bsStyle="info"
              onClick={() => {
                setShowModal(true);
              }}>
              {t('shared.edit_configuration')}
            </Button>
          </IfPermitted>

          <Modal show={showModal && !!formConfig} onHide={resetConfig}>
            <Formik onSubmit={saveConfig} initialValues={formConfig} validate={validate}>
              {({ isSubmitting }) => (
                <Form>
                  <Modal.Header>
                    <Modal.Title>{t('password_complexity.update_title')}</Modal.Title>
                  </Modal.Header>

                  <Modal.Body>
                    <Row>
                      <Col sm={12}>
                        <FormikInput
                          type="number"
                          name="min_length"
                          id="min_length"
                          label={t('password_complexity.min_length')}
                          min={1}
                          required
                          help={t('password_complexity.min_length_help')}
                        />
                      </Col>
                      <Col sm={12}>
                        <FormikInput
                          type="checkbox"
                          name="require_uppercase"
                          id="require_uppercase"
                          label={<LabelSpan>{t('password_complexity.require_uppercase')}</LabelSpan>}
                          help={t('password_complexity.require_uppercase_help')}
                        />
                      </Col>
                      <Col sm={12}>
                        <FormikInput
                          type="checkbox"
                          name="require_lowercase"
                          id="require_lowercase"
                          label={<LabelSpan>{t('password_complexity.require_lowercase')}</LabelSpan>}
                          help={t('password_complexity.require_lowercase_help')}
                        />
                      </Col>
                      <Col sm={12}>
                        <FormikInput
                          type="checkbox"
                          name="require_numbers"
                          id="require_numbers"
                          label={<LabelSpan>{t('password_complexity.require_numbers')}</LabelSpan>}
                          help={t('password_complexity.require_numbers_help')}
                        />
                      </Col>
                      <Col sm={12}>
                        <FormikInput
                          type="checkbox"
                          name="require_special_chars"
                          id="require_special_chars"
                          label={<LabelSpan>{t('password_complexity.require_special_chars')}</LabelSpan>}
                          help={
                            <>
                              {t('password_complexity.require_special_chars_help')}
                              <br />
                              {PASSWORD_SPECIAL_CHARACTERS}
                            </>
                          }
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

export default PasswordComplexityConfig;
