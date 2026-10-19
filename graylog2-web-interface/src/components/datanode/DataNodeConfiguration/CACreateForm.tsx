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
import { useTranslation } from 'react-i18next';
import { useMutation, useQueryClient } from '@tanstack/react-query';

import { CA } from '@graylog/server-api';

import UserNotification from 'util/UserNotification';
import { FormikInput } from 'components/common';
import { Button } from 'components/bootstrap';
import { QUERY_KEY as DATA_NODES_CA_QUERY_KEY } from 'components/datanode/hooks/useDataNodesCA';
import { MIGRATION_STATE_QUERY_KEY } from 'components/datanode/hooks/useMigrationState';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import useProductName from 'brand-customization/useProductName';

type FormValues = {
  organization: string;
};

const createCA = (caData: FormValues) => CA.createCA(caData);

const CaCreateForm = () => {
  const { t } = useTranslation('datanode');
  const queryClient = useQueryClient();
  const sendTelemetry = useSendTelemetry('migration');
  const productName = useProductName();

  const { mutateAsync: onCreateCA } = useMutation({
    mutationFn: createCA,

    onSuccess: () => {
      UserNotification.success(t('ca_create.success'));
      queryClient.invalidateQueries({ queryKey: DATA_NODES_CA_QUERY_KEY });
      queryClient.invalidateQueries({ queryKey: MIGRATION_STATE_QUERY_KEY });
    },

    onError: (error) => {
      UserNotification.error(t('ca_create.failed', { error }));
    },
  });

  const onSubmit = (formValues: FormValues) => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.DATANODE_MIGRATION.CA_CREATE_CA_CLICKED, {
      app_pathname: 'datanode',
    });

    return onCreateCA(formValues).catch(() => {});
  };

  return (
    <div>
      <p>{t('ca_create.description', { productName })}</p>
      <Formik
        initialValues={{ organization: `${productName} CA` }}
        onSubmit={(formValues: FormValues) => onSubmit(formValues)}>
        {({ isSubmitting, isValid }) => (
          <Form>
            <FormikInput
              id="organization"
              placeholder={t('ca_create.organization_name')}
              name="organization"
              label={t('ca_create.organization_name')}
              required
            />
            <Button bsStyle="primary" bsSize="small" disabled={isSubmitting || !isValid} type="submit">
              {isSubmitting ? t('ca_create.submitting') : t('ca_create.submit')}
            </Button>
          </Form>
        )}
      </Formik>
    </div>
  );
};

export default CaCreateForm;
