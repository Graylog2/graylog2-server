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
import { useMemo, useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Field, Form, Formik } from 'formik';
import type { DefaultTheme } from 'styled-components';
import styled, { css } from 'styled-components';
import capitalize from 'lodash/capitalize';
import moment from 'moment';

import ModalSubmit from 'components/common/ModalSubmit';
import UserNotification from 'util/UserNotification';
import { Input, Button, Col, Modal, Row } from 'components/bootstrap';
import { ConfigurationsActions } from 'stores/configurations/ConfigurationsStore';
import { ConfigurationType } from 'components/configurations/ConfigurationTypes';
import { IfPermitted, TimeUnitInput, Spinner } from 'components/common';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import Select from 'components/common/Select';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import { MIGRATION_STATE_QUERY_KEY } from 'components/datanode/hooks/useMigrationState';

import { TIME_UNITS_UPPER } from '../Constants';
import type { TIME_UNITS } from '../Constants';

type RenewalPolicy = {
  mode: 'AUTOMATIC' | 'MANUAL',
  certificate_lifetime: string,
}

type FormConfig = {
  mode: RenewalPolicy['mode'],
  lifetimeUnit: typeof TIME_UNITS[number],
  lifetimeValue: number,
}

const StyledDefList = styled.dl.attrs({
  className: 'deflist',
})(({ theme }: { theme: DefaultTheme }) => css`
  &&.deflist {
    dt {
      float: left;
    }

    dd {
      padding-left: ${theme.spacings.md};
      margin-left: 200px;
    }
  }
`);

const handleSaveConfig = async (configToSave: RenewalPolicy) => (
  ConfigurationsActions.update(ConfigurationType.CERTIFICATE_RENEWAL_POLICY_CONFIG, configToSave)
);

const smallestUnit = (duration: string) => {
  if (duration.endsWith('H')) {
    return 'hours';
  }

  if (duration.endsWith('D')) {
    return 'days';
  }

  if (duration.endsWith('M')) {
    return 'months';
  }

  if (duration.endsWith('Y')) {
    return 'years';
  }

  throw new Error(`Invalid duration specified: ${duration}`);
};

const fetchCurrentConfig = () => ConfigurationsActions.list(ConfigurationType.CERTIFICATE_RENEWAL_POLICY_CONFIG) as Promise<RenewalPolicy>;

const NoExistingPolicy = ({ createPolicy }: { createPolicy: () => void }) => (
  <Button onClick={createPolicy}
          bsSize="small"
          bsStyle="primary">Configure Certificate Renewal Policy
  </Button>
);

const certicateRenewalModes = ['AUTOMATIC', 'MANUAL'].map((mode) => ({ label: capitalize(mode), value: mode }));

const DEFAULT_CONFIG = {
  mode: 'AUTOMATIC',
  lifetimeUnit: 'days',
  lifetimeValue: 30,
} as const;

const queryKey = ['config', 'certificate-renewal-policy'];

const renewalModeExplanation = 'Setting the renewal policy to "Automatic" will '
  + 'renew all expiring certificates without any user interaction. Setting it to "Manual" will create a system '
  + 'notification when one or more certificates are about to expire, allowing you to confirm their renewal.';
const lifetimeExplanation = 'The certificate lifetime will be used for the length of the validity of newly created certificates.';

type Props = {
  className?: string
}

const CertificateRenewalPolicyConfig = ({ className = undefined }: Props) => {
  const [showModal, setShowModal] = useState<boolean>(false);
  const { data: currentConfig, isLoading } = useQuery(queryKey, fetchCurrentConfig);

  const sendTelemetry = useSendTelemetry();
  const queryClient = useQueryClient();

  const { mutateAsync: updateConfig } = useMutation(handleSaveConfig, {
    onSuccess: () => {
      queryClient.invalidateQueries(queryKey);
      queryClient.invalidateQueries(MIGRATION_STATE_QUERY_KEY);
      setShowModal(false);
    },
    onError: (err: Error) => {
      UserNotification.error(`Error Updating Detector Definition: ${err.toString()}`, 'Unable to update detector definition');
    },
  });

  const formConfig: FormConfig | undefined = useMemo(() => {
    if (isLoading) {
      return undefined;
    }

    if (!currentConfig) {
      return DEFAULT_CONFIG;
    }

    const { mode, certificate_lifetime } = currentConfig;
    const lifetimeUnit = smallestUnit(certificate_lifetime);
    const lifetimeValue = moment.duration(certificate_lifetime).as(lifetimeUnit);

    return {
      mode,
      lifetimeUnit,
      lifetimeValue,
    };
  }, [currentConfig, isLoading]);

  if (isLoading) {
    return <Spinner />;
  }

  const modalTitle = 'Configure Certificate Renewal Policy';

  const resetConfig = () => {
    setShowModal(false);
  };

  const saveConfig = (values: FormConfig) => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.DATANODE_MIGRATION.CR_UPDATE_CONFIGURATION_CLICKED, {
      app_pathname: 'datanode',
      app_section: 'migration',
    });

    const newConfig = {
      mode: values.mode,
      certificate_lifetime: moment.duration(values.lifetimeValue, values.lifetimeUnit).toJSON(),
    };

    return updateConfig(newConfig);
  };

  return (
    <div className={className}>
      <h2>Certificate Renewal Policy Configuration</h2>
      <p>
        These settings will be used when detecting expiration of certificates and/or when renewing them.
      </p>
      {!currentConfig ? <NoExistingPolicy createPolicy={() => setShowModal(true)} /> : (
        <>
          <StyledDefList>
            <dt>Renewal Mode:</dt>
            <dd>{capitalize(currentConfig.mode)}</dd>
            <dd><i>{renewalModeExplanation}</i></dd>
            <dt>Certificate Lifetime:</dt>
            <dd>{formConfig.lifetimeValue} {formConfig.lifetimeUnit}</dd>
            <dd><i>{lifetimeExplanation}</i></dd>
          </StyledDefList>

          <p className="no-bm">
            <IfPermitted permissions="indices:changestate">
              <Button bsStyle="primary"
                      bsSize="small"
                      onClick={() => {
                        sendTelemetry(TELEMETRY_EVENT_TYPE.DATANODE_MIGRATION.CR_EDIT_CONFIGURATION_CLICKED, {
                          app_pathname: 'datanode',
                          app_section: 'migration',
                        });

                        setShowModal(true);
                      }}>Edit configuration
              </Button>
            </IfPermitted>
          </p>
        </>
      )}

      <Modal show={showModal}
             onHide={resetConfig}
             aria-modal="true"
             aria-labelledby="dialog_label">
        <Formik<FormConfig> onSubmit={saveConfig} initialValues={formConfig}>
          {({ values, setFieldValue, isSubmitting, isValid, isValidating }) => (
            <Form>
              <Modal.Header closeButton>
                <Modal.Title id="dialog_label">{modalTitle}</Modal.Title>
              </Modal.Header>

              <Modal.Body>
                <div>
                  <Row>
                    <Col md={12}>
                      <Field name="mode">
                        {({ field: { name, value, onChange } }) => (
                          <Input id={name}
                                 label="Certificate Renewal Mode"
                                 help={renewalModeExplanation}>
                            <Select options={certicateRenewalModes}
                                    clearable={false}
                                    name={name}
                                    value={value ?? 'AUTOMATIC'}
                                    aria-label="Select certificate renewal mode"
                                    size="small"
                                    onChange={(newValue) => onChange({ target: { name, value: newValue } })} />
                          </Input>
                        )}
                      </Field>
                      <TimeUnitInput label="Certificate Lifetime"
                                     help={lifetimeExplanation}
                                     update={(value, unit) => {
                                       setFieldValue('lifetimeValue', value);
                                       setFieldValue('lifetimeUnit', unit);
                                     }}
                                     value={values.lifetimeValue}
                                     unit={values.lifetimeUnit.toLocaleUpperCase()}
                                     enabled
                                     hideCheckbox
                                     units={TIME_UNITS_UPPER} />
                    </Col>
                  </Row>
                </div>
              </Modal.Body>

              <Modal.Footer>
                <ModalSubmit onCancel={resetConfig}
                             disabledSubmit={isValidating || !isValid}
                             isSubmitting={isSubmitting}
                             isAsyncSubmit
                             submitLoadingText="Updating configuration"
                             submitButtonText="Update configuration" />
              </Modal.Footer>
            </Form>
          )}
        </Formik>
      </Modal>
    </div>
  );
};

export default CertificateRenewalPolicyConfig;
