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
import { useCallback, useState } from 'react';
import styled, { css } from 'styled-components';
import { Formik, Form } from 'formik';

import { Button } from 'components/bootstrap';
import { Card, ConfirmDialog, FormikInput, RelativeTime } from 'components/common';
import FormSubmit from 'components/common/FormSubmit';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

import useSendCollectorsTelemetry from '../hooks/useSendCollectorsTelemetry';
import type { Fleet } from '../types';

type Props = {
  fleet: Fleet;
  onSave: (updates: Partial<Fleet>) => Promise<void>;
  onDelete?: () => Promise<void>;
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

const WarningText = styled.p`
  font-size: ${({ theme }) => theme.fonts.size.small};
  color: ${({ theme }) => theme.colors.gray[60]};
  margin-bottom: ${({ theme }) => theme.spacings.sm};
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
    errors.name = 'Name is required';
  }

  return errors;
};

const FleetSettings = ({ fleet, onSave, onDelete = undefined }: Props) => {
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const sendTelemetry = useSendCollectorsTelemetry();

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

  const handleConfirmDelete = useCallback(async () => {
    await onDelete?.();
    sendTelemetry(TELEMETRY_EVENT_TYPE.COLLECTORS.FLEET.DELETED, {
      app_action_value: 'fleet-delete',
      fleet_id: fleet.id,
    });
  }, [fleet.id, onDelete, sendTelemetry]);

  return (
    <div>
      <Formik<FormValues> initialValues={initialValues} onSubmit={handleSubmit} validate={validate} enableReinitialize>
        {({ isSubmitting, isValidating, dirty, resetForm }) => (
          <Section>
            <SectionTitle>General Settings</SectionTitle>
            <Form>
              <FormikInput id="fleet-name" label="Fleet Name" name="name" required />
              <FormikInput id="fleet-description" type="textarea" label="Description" name="description" />
              <FormSubmit
                isAsyncSubmit
                submitButtonText="Save changes"
                submitLoadingText="Saving..."
                isSubmitting={isSubmitting}
                disabledSubmit={!dirty || isValidating}
                onCancel={resetForm}
                disabledCancel={!dirty || isSubmitting}
              />
            </Form>
          </Section>
        )}
      </Formik>

      <Section>
        <SectionTitle>Fleet Information</SectionTitle>
        <div>
          <InfoRow>
            <InfoLabel>Fleet ID:</InfoLabel>
            <InfoValue>{fleet.id}</InfoValue>
          </InfoRow>
          <InfoRow>
            <InfoLabel>Created:</InfoLabel>
            <RelativeTime dateTime={fleet.created_at} />
          </InfoRow>
          <InfoRow>
            <InfoLabel>Updated:</InfoLabel>
            <RelativeTime dateTime={fleet.updated_at} />
          </InfoRow>
        </div>
      </Section>

      <Section>
        <SectionTitle>Danger Zone</SectionTitle>
        <WarningText>
          Deleting a fleet removes all source configurations and unenrolls all collector instances.
          Instances will stop collecting data and must be re-enrolled into a new fleet.
        </WarningText>
        <Button bsStyle="danger" onClick={() => setShowDeleteConfirm(true)} disabled={!onDelete}>
          Delete Fleet
        </Button>
      </Section>

      {showDeleteConfirm && (
        <ConfirmDialog
          title="Delete fleet"
          show
          onConfirm={handleConfirmDelete}
          onCancel={() => setShowDeleteConfirm(false)}>
          Are you sure you want to delete fleet <strong>{fleet.name}</strong>? All source configurations will be
          removed and collector instances will stop collecting data. Instances must be re-enrolled into a new fleet.
        </ConfirmDialog>
      )}
    </div>
  );
};

export default FleetSettings;
