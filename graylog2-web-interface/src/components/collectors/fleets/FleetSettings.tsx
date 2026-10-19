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
import { useCallback } from 'react';
import styled, { css } from 'styled-components';
import { Formik, Form } from 'formik';
import { useTranslation } from 'react-i18next';

import i18n from 'i18n';
import { Card, FormikInput, RelativeTime } from 'components/common';
import FormSubmit from 'components/common/FormSubmit';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

import { useCollectorPermissions } from '../hooks';
import useSendCollectorsTelemetry from '../hooks/useSendCollectorsTelemetry';
import type { Fleet } from '../types';

type Props = {
  fleet: Fleet;
  onSave: (updates: Partial<Fleet>) => Promise<void>;
};

type FormValues = {
  name: string;
  description: string;
};

const Section = styled(Card)`
  margin-bottom: ${({ theme }) => theme.spacings.md};
`;

const InfoRow = styled.div`
  display: flex;
  align-items: center;
  margin-bottom: ${({ theme }) => theme.spacings.xs};
`;

const InfoLabel = styled.span`
  font-size: ${({ theme }) => theme.fonts.size.small};
  font-weight: 500;
  min-width: 100px;
`;

const InfoValue = styled.span`
  font-size: ${({ theme }) => theme.fonts.size.small};
  font-family: ${({ theme }) => theme.fonts.family.monospace};
`;

const SectionTitle = styled.h4(
  ({ theme }) => css`
    margin-bottom: ${theme.spacings.sm};
    font-size: ${theme.fonts.size.body};
    font-weight: 600;
  `,
);

const validate = (values: FormValues) => {
  const errors: Partial<Record<keyof FormValues, string>> = {};

  if (!values.name) {
    errors.name = i18n.t('fleet_form.name_required', { ns: 'collectors' });
  }

  return errors;
};

const FleetSettings = ({ fleet, onSave }: Props) => {
  const { t } = useTranslation('collectors');
  const sendTelemetry = useSendCollectorsTelemetry();
  const { canEditFleet } = useCollectorPermissions();
  const canEdit = canEditFleet(fleet.id);

  const initialValues: FormValues = {
    name: fleet.name,
    description: fleet.description || '',
  };

  const handleSubmit = useCallback(
    async (values: FormValues) => {
      await onSave({
        name: values.name,
        description: values.description,
      });
      sendTelemetry(TELEMETRY_EVENT_TYPE.COLLECTORS.FLEET.UPDATED, {
        app_action_value: 'fleet-settings-save',
        fleet_id: fleet.id,
      });
    },
    [fleet.id, onSave, sendTelemetry],
  );

  return (
    <div>
      <Formik<FormValues> initialValues={initialValues} onSubmit={handleSubmit} validate={validate} enableReinitialize>
        {({ isSubmitting, isValidating, dirty, resetForm }) => (
          <Section>
            <SectionTitle>{t('fleet_settings.general')}</SectionTitle>
            <Form>
              <FormikInput
                id="fleet-name"
                label={t('fleet_settings.fleet_name')}
                name="name"
                required
                disabled={!canEdit}
              />
              <FormikInput
                id="fleet-description"
                type="textarea"
                label={t('fleet_form.description')}
                name="description"
                disabled={!canEdit}
              />
              {canEdit && (
                <FormSubmit
                  isAsyncSubmit
                  submitButtonText={t('fleet_settings.save')}
                  submitLoadingText={t('fleet_settings.saving')}
                  isSubmitting={isSubmitting}
                  disabledSubmit={!dirty || isValidating}
                  onCancel={resetForm}
                  disabledCancel={!dirty || isSubmitting}
                />
              )}
            </Form>
          </Section>
        )}
      </Formik>

      <Section>
        <SectionTitle>{t('fleet_settings.information')}</SectionTitle>
        <div>
          <InfoRow>
            <InfoLabel>{t('fleet_settings.fleet_id')}</InfoLabel>
            <InfoValue>{fleet.id}</InfoValue>
          </InfoRow>
          <InfoRow>
            <InfoLabel>{t('fleet_settings.created')}</InfoLabel>
            <RelativeTime dateTime={fleet.created_at} />
          </InfoRow>
          <InfoRow>
            <InfoLabel>{t('fleet_settings.updated')}</InfoLabel>
            <RelativeTime dateTime={fleet.updated_at} />
          </InfoRow>
        </div>
      </Section>
    </div>
  );
};

export default FleetSettings;
