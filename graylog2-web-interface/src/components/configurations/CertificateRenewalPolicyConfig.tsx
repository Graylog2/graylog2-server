import * as React from 'react';
import { useMemo, useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Field, Form, Formik } from 'formik';
import type { DefaultTheme } from 'styled-components';
import styled, { css } from 'styled-components';
import capitalize from 'lodash/capitalize';
import moment from 'moment';

import UserNotification from 'util/UserNotification';
import { Input, Button, Col, Modal, Row } from 'components/bootstrap';
import { ConfigurationsActions } from 'stores/configurations/ConfigurationsStore';
import { ConfigurationType } from 'components/configurations/ConfigurationTypes';
import { IfPermitted, TimeUnitInput, Spinner } from 'components/common';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import Select from 'components/common/Select';

type RenewalPolicy = {
  mode: 'AUTOMATIC' | 'MANUAL',
  certificate_lifetime: string,
}
const TIME_UNITS = ['hours', 'days', 'months', 'years'] as const;
const TIME_UNITS_UPPER = TIME_UNITS.map((unit) => unit.toLocaleUpperCase());

type FormConfig = {
  mode: RenewalPolicy['mode'],
  lifetimeUnit: typeof TIME_UNITS[number],
  lifetimeValue: number,
}

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
  <span>There is no Certificate Renewal Policy yet. Click <a role="button" onClick={createPolicy}>here</a> to create one.</span>
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

const CertificateRenewalPolicyConfig = () => {
  const [showModal, setShowModal] = useState<boolean>(false);
  const { data: currentConfig, isLoading } = useQuery(queryKey, fetchCurrentConfig);

  const sendTelemetry = useSendTelemetry();
  const queryClient = useQueryClient();

  const { mutateAsync: updateConfig } = useMutation(handleSaveConfig, {
    onSuccess: () => {
      queryClient.invalidateQueries(queryKey);
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
    sendTelemetry('form_submit', {
      app_pathname: 'configurations',
      app_section: 'certificate-renewal-policy',
      app_action_value: 'configuration-save',
    });

    const newConfig = {
      mode: values.mode,
      certificate_lifetime: moment.duration(values.lifetimeValue, values.lifetimeUnit).toJSON(),
    };

    return updateConfig(newConfig);
  };

  return (
    <div>
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

          <p>
            <IfPermitted permissions="indices:changestate">
              <Button bsStyle="info"
                      bsSize="xs"
                      onClick={() => {
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
             aria-labelledby="dialog_label"
             data-app-section="configurations_index_defaults"
             data-event-element={modalTitle}>
        <Formik<FormConfig> onSubmit={saveConfig} initialValues={formConfig}>
          {({ values, setFieldValue, isSubmitting }) => (
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
                <Button type="button" onClick={resetConfig}>Cancel</Button>
                <Button type="submit"
                        bsStyle="success"
                        disabled={isSubmitting}>{isSubmitting ? 'Updating configuration' : 'Update configuration'}
                </Button>
              </Modal.Footer>
            </Form>
          )}
        </Formik>
      </Modal>
    </div>
  );
};

export default CertificateRenewalPolicyConfig;
