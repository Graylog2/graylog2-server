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

import type { ConfigurationCategory, ConfigurationFormValues } from './types';
import {
  buildConfigurationFormValues,
  buildConfigurationValuesPatch,
  validateConfigurationFormValues,
} from './configurationFields';
import { useUpdateManagedConfiguration } from './useManagedConfiguration';
import ConfigurationFieldInput from './ConfigurationFieldInput';

type Props = {
  category: ConfigurationCategory;
  nodeId: string | null;
};

const DataNodeConfigurationForm = ({ category, nodeId }: Props) => {
  const { isPermitted } = usePermissions();
  const { updateConfiguration } = useUpdateManagedConfiguration(nodeId);
  const parameters = category.parameters;
  const initialValues = buildConfigurationFormValues(parameters);
  const restartDescription =
    nodeId === null
      ? 'Changing this setting requires restarting the Data Nodes for the change to take effect.'
      : 'Changing this setting requires restarting this Data Node for the change to take effect.';
  const canDelete = isPermitted('clusterconfigentry:delete');
  const canEdit = isPermitted(['clusterconfigentry:create', 'clusterconfigentry:edit']);

  const formik = useFormik<ConfigurationFormValues>({
    initialValues,
    validateOnChange: false,
    validate: (values) => validateConfigurationFormValues(values, parameters),
    onSubmit: async (values, { resetForm, setStatus }) => {
      setStatus(undefined);

      try {
        const saved = await updateConfiguration(buildConfigurationValuesPatch(formik.initialValues, values));
        const savedParameters = saved.parameters.filter(({ category: it }) => it === category.label);
        resetForm({ values: buildConfigurationFormValues(savedParameters) });
        UserNotification.success('Configuration saved.');
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
        <section aria-labelledby={`data-node-configuration-${category.id}`}>
          <h3 id={`data-node-configuration-${category.id}`}>{category.label}</h3>
          <hr />
          {parameters.map((parameter) => (
            <ConfigurationFieldInput
              key={parameter.name}
              parameter={parameter}
              canEdit={canEdit}
              canDelete={canDelete}
              restartDescription={restartDescription}
            />
          ))}
        </section>
        <p>Saving stores the values. Applying them to running Data Nodes is not available yet.</p>
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
