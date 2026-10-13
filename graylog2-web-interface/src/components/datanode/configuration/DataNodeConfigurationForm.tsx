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
import React from 'react';
import { Form, FormikProvider, useFormik } from 'formik';

import { Alert } from 'components/bootstrap';
import { ConfirmLeaveDialog, FormSubmit } from 'components/common';
import usePermissions from 'hooks/usePermissions';
import UserNotification from 'util/UserNotification';

import type {
  ConfigurationFormValues,
  ConfigurationGroup,
  ConfigurationOverrides,
  ConfigurationOverrideValue,
} from './types';
import getConfigurationFields, {
  buildConfigurationFormValues,
  buildConfigurationOverridesPatch,
  validateConfigurationFormValues,
} from './configurationFields';
import { useUpdateConfigurationOverrides } from './useConfigurationOverrides';
import ConfigurationFieldInput from './ConfigurationFieldInput';

type Props = {
  configuration: ConfigurationOverrides;
  group: ConfigurationGroup;
  inheritedOverrides?: Record<string, ConfigurationOverrideValue>;
};

const DataNodeConfigurationForm = ({ configuration, group, inheritedOverrides = {} }: Props) => {
  const { isPermitted } = usePermissions();
  const { updateOverrides } = useUpdateConfigurationOverrides(configuration.node_id);
  const fields = getConfigurationFields(configuration.node_id).filter((field) => field.group === group.id);
  const initialValues = buildConfigurationFormValues(configuration.overrides, fields);
  const inheritedValues = buildConfigurationFormValues(inheritedOverrides, fields);
  const restartDescription =
    configuration.node_id === null
      ? 'Changing this setting requires restarting the Data Nodes for the change to take effect.'
      : 'Changing this setting requires restarting this Data Node for the change to take effect.';
  const canDelete = isPermitted('clusterconfigentry:delete');
  const canEdit = isPermitted(['clusterconfigentry:create', 'clusterconfigentry:edit']);

  const formik = useFormik<ConfigurationFormValues>({
    initialValues,
    validateOnChange: false,
    validate: (values) => validateConfigurationFormValues(values, fields),
    onSubmit: async (values, { resetForm, setStatus }) => {
      setStatus(undefined);
      try {
        const saved = await updateOverrides(buildConfigurationOverridesPatch(formik.initialValues, values));
        resetForm({ values: buildConfigurationFormValues(saved.overrides, fields) });
        UserNotification.success('Configuration overrides saved.');
      } catch {
        setStatus('Could not save Data Node configuration. Your changes have been kept. Please try again.');
      }
    },
  });
  const { dirty, isSubmitting, resetForm, status } = formik;

  return (
    <FormikProvider value={formik}>
      <Form noValidate>
        {dirty && !isSubmitting && <ConfirmLeaveDialog question="Discard your unsaved configuration changes?" />}
        {status && <Alert bsStyle="danger">{status}</Alert>}
        <section aria-labelledby={`data-node-configuration-${group.id}`}>
          <h3 id={`data-node-configuration-${group.id}`}>{group.label}</h3>
          <hr />
          <p>{group.description}</p>
          <p>Only saved overrides are shown. Values from configuration files and running nodes are unavailable.</p>
          {fields.map((field) => (
            <ConfigurationFieldInput
              key={field.key}
              field={field}
              canEdit={canEdit}
              canDelete={canDelete || !Object.hasOwn(formik.initialValues, field.key)}
              inheritedValue={inheritedValues[field.key]}
              restartDescription={restartDescription}
            />
          ))}
        </section>
        <p>Saving stores overrides. Applying changes and restarting Data Nodes are not available yet.</p>
        {canEdit && (
          <FormSubmit
            submitButtonText="Save changes"
            isAsyncSubmit
            isSubmitting={isSubmitting}
            submitLoadingText="Saving changes..."
            disabledSubmit={!dirty}
            disabledCancel={!dirty}
            onCancel={() => resetForm()}
          />
        )}
      </Form>
    </FormikProvider>
  );
};

export default DataNodeConfigurationForm;
