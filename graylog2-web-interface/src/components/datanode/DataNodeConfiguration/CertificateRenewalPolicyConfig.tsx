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
import moment from 'moment';
import { useTranslation } from 'react-i18next';

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
  mode: 'AUTOMATIC' | 'MANUAL';
  certificate_lifetime: string;
};

type FormConfig = {
  mode: RenewalPolicy['mode'];
  lifetimeUnit: (typeof TIME_UNITS)[number];
  lifetimeValue: number;
};

const StyledDefList = styled.dl.attrs({
  className: 'deflist',
})(
  ({ theme }: { theme: DefaultTheme }) => css`
    &&.deflist {
      dt {
        float: left;
      }

      dd {
        padding-left: ${theme.spacings.md};
        margin-left: 200px;
      }
    }
  `,
);

const handleSaveConfig = async (configToSave: RenewalPolicy) =>
  ConfigurationsActions.update(ConfigurationType.CERTIFICATE_RENEWAL_POLICY_CONFIG, configToSave);

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

const fetchCurrentConfig = () =>
  ConfigurationsActions.list(ConfigurationType.CERTIFICATE_RENEWAL_POLICY_CONFIG) as Promise<RenewalPolicy>;

const NoExistingPolicy = ({ createPolicy }: { createPolicy: () => void }) => {
  const { t } = useTranslation('datanode');

  return (
    <Button onClick={createPolicy} bsSize="small" bsStyle="primary">
      {t('renewal_policy.configure')}
    </Button>
  );
};

const RENEWAL_MODE_KEYS = {
  AUTOMATIC: 'renewal_policy.modes.automatic',
  MANUAL: 'renewal_policy.modes.manual',
} as const;

const DEFAULT_CONFIG = {
  mode: 'AUTOMATIC',
  lifetimeUnit: 'days',
  lifetimeValue: 30,
} as const;

const queryKey = ['config', 'certificate-renewal-policy'];

type Props = {
  className?: string;
};

const CertificateRenewalPolicyConfig = ({ className = undefined }: Props) => {
  const { t } = useTranslation('datanode');
  const [showModal, setShowModal] = useState<boolean>(false);
  const { data: currentConfig, isLoading } = useQuery({
    queryKey: queryKey,
    queryFn: fetchCurrentConfig,
  });

  const sendTelemetry = useSendTelemetry('migration');
  const queryClient = useQueryClient();

  const { mutateAsync: updateConfig } = useMutation({
    mutationFn: handleSaveConfig,

    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKey,
      });
      queryClient.invalidateQueries({ queryKey: MIGRATION_STATE_QUERY_KEY });
      setShowModal(false);
    },

    onError: (err: Error) => {
      UserNotification.error(
        t('renewal_policy.update_failed', { error: err.toString() }),
        t('renewal_policy.update_failed_title'),
      );
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

  const modalTitle = t('renewal_policy.configure');
  const renewalModeExplanation = t('renewal_policy.mode_explanation');
  const lifetimeExplanation = t('renewal_policy.lifetime_explanation');
  const certificateRenewalModes = (['AUTOMATIC', 'MANUAL'] as const).map((mode) => ({
    label: t(RENEWAL_MODE_KEYS[mode]),
    value: mode,
  }));

  const resetConfig = () => {
    setShowModal(false);
  };

  const saveConfig = (values: FormConfig) => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.DATANODE_MIGRATION.CR_UPDATE_CONFIGURATION_CLICKED, {
      app_pathname: 'datanode',
    });

    const newConfig = {
      mode: values.mode,
      certificate_lifetime: moment.duration(values.lifetimeValue, values.lifetimeUnit).toJSON(),
    };

    return updateConfig(newConfig);
  };

  return (
    <div className={className}>
      <h2>{t('renewal_policy.title')}</h2>
      <p>{t('renewal_policy.description')}</p>
      {!currentConfig ? (
        <NoExistingPolicy createPolicy={() => setShowModal(true)} />
      ) : (
        <>
          <StyledDefList>
            <dt>{t('renewal_policy.renewal_mode')}</dt>
            <dd>{t(RENEWAL_MODE_KEYS[currentConfig.mode])}</dd>
            <dd>
              <i>{renewalModeExplanation}</i>
            </dd>
            <dt>{t('renewal_policy.certificate_lifetime')}</dt>
            <dd>{t(`common:duration.${formConfig.lifetimeUnit}`, { count: formConfig.lifetimeValue })}</dd>
            <dd>
              <i>{lifetimeExplanation}</i>
            </dd>
          </StyledDefList>

          <p className="no-bm">
            <IfPermitted permissions="indices:changestate">
              <Button
                bsStyle="primary"
                bsSize="small"
                onClick={() => {
                  sendTelemetry(TELEMETRY_EVENT_TYPE.DATANODE_MIGRATION.CR_EDIT_CONFIGURATION_CLICKED, {
                    app_pathname: 'datanode',
                  });

                  setShowModal(true);
                }}>
                {t('renewal_policy.edit')}
              </Button>
            </IfPermitted>
          </p>
        </>
      )}

      <Modal show={showModal} onHide={resetConfig}>
        <Formik<FormConfig> onSubmit={saveConfig} initialValues={formConfig}>
          {({ values, setFieldValue, isSubmitting, isValid, isValidating }) => (
            <Form>
              <Modal.Header>
                <Modal.Title>{modalTitle}</Modal.Title>
              </Modal.Header>

              <Modal.Body>
                <div>
                  <Row>
                    <Col md={12}>
                      <Field name="mode">
                        {({ field: { name, value, onChange } }) => (
                          <Input id={name} label={t('renewal_policy.renewal_mode_label')} help={renewalModeExplanation}>
                            <Select
                              options={certificateRenewalModes}
                              clearable={false}
                              name={name}
                              value={value ?? 'AUTOMATIC'}
                              aria-label={t('renewal_policy.select_mode')}
                              size="small"
                              onChange={(newValue) => onChange({ target: { name, value: newValue } })}
                            />
                          </Input>
                        )}
                      </Field>
                      <TimeUnitInput
                        label={t('renewal_policy.certificate_lifetime_label')}
                        help={lifetimeExplanation}
                        update={(value, unit) => {
                          setFieldValue('lifetimeValue', value);
                          setFieldValue('lifetimeUnit', unit);
                        }}
                        value={values.lifetimeValue}
                        unit={values.lifetimeUnit.toLocaleUpperCase()}
                        enabled
                        hideCheckbox
                        units={TIME_UNITS_UPPER}
                      />
                    </Col>
                  </Row>
                </div>
              </Modal.Body>

              <Modal.Footer>
                <ModalSubmit
                  onCancel={resetConfig}
                  disabledSubmit={isValidating || !isValid}
                  isSubmitting={isSubmitting}
                  isAsyncSubmit
                  submitLoadingText={t('renewal_policy.submitting')}
                  submitButtonText={t('renewal_policy.submit')}
                />
              </Modal.Footer>
            </Form>
          )}
        </Formik>
      </Modal>
    </div>
  );
};

export default CertificateRenewalPolicyConfig;
