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
import type { PasswordComplexityConfigType } from 'stores/configurations/ConfigurationsStore';
import { ConfigurationType } from 'components/configurations/ConfigurationTypes';
import { getConfig } from 'components/configurations/helpers';
import { useStore } from 'stores/connect';
import type { Store } from 'stores/StoreTypes';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import useLocation from 'routing/useLocation';
import { getPathnameWithoutId } from 'util/URLUtils';
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
  const [showModal, setShowModal] = useState<boolean>(false);
  const [viewConfig, setViewConfig] = useState<PasswordComplexityConfigType | undefined>(undefined);
  const [formConfig, setFormConfig] = useState<PasswordComplexityConfigType | undefined>(undefined);
  const configuration = useStore(ConfigurationsStore as Store<Record<string, any>>, (state) => state?.configuration);

  const sendTelemetry = useSendTelemetry();
  const { pathname } = useLocation();

  useEffect(() => {
    ConfigurationsActions.listPasswordComplexityConfig(configType).then(() => {
      const config = getConfig(configType, configuration) ?? DEFAULT_PASSWORD_COMPLEXITY_CONFIG;

      setViewConfig(config);
      setFormConfig(config);
    });
  }, [configuration]);

  const saveConfig = (values: PasswordComplexityConfigType) => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.CONFIGURATIONS.PASSWORD_COMPLEXITY_UPDATED, {
      app_pathname: getPathnameWithoutId(pathname),
      app_section: 'password-complexity',
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
      errors.min_length = 'Minimum length must be at least 1';
    }

    return errors;
  };

  const renderRequirement = (label: string, value: React.ReactNode) => (
    <>
      <dt>{label}</dt>
      <dd aria-label={label}>{value}</dd>
    </>
  );

  const requirementLabel = (required: boolean) => (required ? 'Required' : 'Not required');

  return (
    <div>
      <h2>Password Complexity</h2>
      <p>Configure the minimum password length and character requirements enforced for local users.</p>

      {!viewConfig ? (
        <Spinner />
      ) : (
        <>
          <StyledDefList>
            {renderRequirement('Minimum length:', viewConfig.min_length)}
            {renderRequirement('Require uppercase letters:', requirementLabel(viewConfig.require_uppercase))}
            {renderRequirement('Require lowercase letters:', requirementLabel(viewConfig.require_lowercase))}
            {renderRequirement('Require numbers:', requirementLabel(viewConfig.require_numbers))}
            {renderRequirement('Require special characters:', requirementLabel(viewConfig.require_special_chars))}
          </StyledDefList>

          <IfPermitted permissions="clusterconfigentry:edit">
            <Button
              type="button"
              bsSize="xs"
              bsStyle="info"
              onClick={() => {
                setShowModal(true);
              }}>
              Edit configuration
            </Button>
          </IfPermitted>

          <Modal show={showModal && !!formConfig} onHide={resetConfig}>
            <Formik onSubmit={saveConfig} initialValues={formConfig} validate={validate}>
              {({ isSubmitting }) => (
                <Form>
                  <Modal.Header>
                    <Modal.Title>Update Password Complexity</Modal.Title>
                  </Modal.Header>

                  <Modal.Body>
                    <Row>
                      <Col sm={12}>
                        <FormikInput
                          type="number"
                          name="min_length"
                          id="min_length"
                          label="Minimum length"
                          min={1}
                          required
                          help="Passwords shorter than this length will be rejected."
                        />
                      </Col>
                      <Col sm={12}>
                        <FormikInput
                          type="checkbox"
                          name="require_uppercase"
                          id="require_uppercase"
                          label={<LabelSpan>Require uppercase letters</LabelSpan>}
                          help="Password must include at least one uppercase character (A-Z)."
                        />
                      </Col>
                      <Col sm={12}>
                        <FormikInput
                          type="checkbox"
                          name="require_lowercase"
                          id="require_lowercase"
                          label={<LabelSpan>Require lowercase letters</LabelSpan>}
                          help="Password must include at least one lowercase character (a-z)."
                        />
                      </Col>
                      <Col sm={12}>
                        <FormikInput
                          type="checkbox"
                          name="require_numbers"
                          id="require_numbers"
                          label={<LabelSpan>Require numbers</LabelSpan>}
                          help="Password must include at least one numeric character (0-9)."
                        />
                      </Col>
                      <Col sm={12}>
                        <FormikInput
                          type="checkbox"
                          name="require_special_chars"
                          id="require_special_chars"
                          label={<LabelSpan>Require special characters</LabelSpan>}
                          help={
                            <>
                              Password must include at least one of the following characters:
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

export default PasswordComplexityConfig;
