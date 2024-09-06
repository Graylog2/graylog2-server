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
import { Formik, Form } from 'formik';
import { useMutation, useQueryClient } from '@tanstack/react-query';

import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import UserNotification from 'util/UserNotification';
import { FormikInput } from 'components/common';
import { Button } from 'components/bootstrap';
import { QUERY_KEY as DATA_NODES_CA_QUERY_KEY } from 'components/datanode/hooks/useDataNodesCA';
import { MIGRATION_STATE_QUERY_KEY } from 'components/datanode/hooks/useMigrationState';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

type FormValues = {
  organization: string
}

const createCA = (caData: FormValues) => fetch(
  'POST',
  qualifyUrl('ca/create'),
  caData,
  false,
);

const CaCreateForm = () => {
  const queryClient = useQueryClient();
  const sendTelemetry = useSendTelemetry();

  const { mutateAsync: onCreateCA } = useMutation(createCA, {
    onSuccess: () => {
      UserNotification.success('CA created successfully');
      queryClient.invalidateQueries(DATA_NODES_CA_QUERY_KEY);
      queryClient.invalidateQueries(MIGRATION_STATE_QUERY_KEY);
    },
    onError: (error) => {
      UserNotification.error(`CA creation failed with error: ${error}`);
    },
  });

  const onSubmit = (formValues: FormValues) => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.DATANODE_MIGRATION.CA_CREATE_CA_CLICKED, {
      app_pathname: 'datanode',
      app_section: 'migration',
    });

    return onCreateCA(formValues).catch(() => {});
  };

  return (
    <div>
      <p>
        Click on the &ldquo;Create CA&rdquo; button to quickly create a new certificate authority for your Graylog Data Nodes.
      </p>
      <Formik initialValues={{ organization: 'Graylog CA' }} onSubmit={(formValues: FormValues) => onSubmit(formValues)}>
        {({ isSubmitting, isValid }) => (
          <Form>
            <FormikInput id="organization"
                         placeholder="Organization Name"
                         name="organization"
                         label="Organization Name"
                         required />
            <Button bsStyle="primary" bsSize="small" disabled={isSubmitting || !isValid} type="submit">
              {isSubmitting ? 'Creating CA...' : 'Create CA'}
            </Button>
          </Form>
        )}
      </Formik>
    </div>
  );
};

export default CaCreateForm;
