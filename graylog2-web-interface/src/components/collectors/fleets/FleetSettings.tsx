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

import type { Fleet } from '../types';

type Props = {
  fleet: Fleet;
  onSave: (updates: Partial<Fleet>) => Promise<void>;
  onDelete?: () => Promise<void>;
};

type FormValues = {
  name: string;
  description: string;
  target_version: string;
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

  const initialValues: FormValues = {
    name: fleet.name,
    description: fleet.description || '',
    target_version: fleet.target_version ?? '',
  };

  const handleSubmit = useCallback(
    async (values: FormValues) => {
      await onSave({
        name: values.name,
        description: values.description,
        target_version: values.target_version || null,
      });
    },
    [onSave],
  );

  const handleConfirmDelete = useCallback(async () => {
    await onDelete?.();
  }, [onDelete]);

  return (
    <div>
      <Formik<FormValues> initialValues={initialValues} onSubmit={handleSubmit} validate={validate} enableReinitialize>
        {({ isSubmitting, isValidating, dirty, resetForm }) => (
          <Section>
            <SectionTitle>General Settings</SectionTitle>
            <Form>
              <FormikInput id="fleet-name" label="Fleet Name" name="name" required />
              <FormikInput id="fleet-description" type="textarea" label="Description" name="description" />
              <FormikInput
                id="fleet-target-version"
                label="Target Version"
                name="target_version"
                help="Collector version to deploy to this fleet"
                placeholder="e.g., 1.2.0"
              />
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
          Deleting a fleet will remove all configuration. Instances will need to be re-enrolled.
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
          Are you sure you want to delete fleet <strong>{fleet.name}</strong>? All configuration will be removed and
          instances will need to be re-enrolled.
        </ConfirmDialog>
      )}
    </div>
  );
};

export default FleetSettings;
