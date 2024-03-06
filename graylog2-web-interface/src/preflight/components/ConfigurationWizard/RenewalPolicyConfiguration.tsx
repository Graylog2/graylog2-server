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
import { Formik, Form, Field } from 'formik';
import styled from 'styled-components';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import moment from 'moment';

import { Title, Space, Button, Group, NumberInput, Input } from 'preflight/components/common';
import UserNotification from 'preflight/util/UserNotification';
import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import Select from 'preflight/components/common/Select';
import { QUERY_KEY as RENEWAL_POLICY_QUERY_KEY } from 'preflight/hooks/useRenewalPolicy';

type FormValues = {
  renewal_policy: 'Automatic' | 'Manual',
  lifetime_value: number,
  lifetime_unit: 'hours' | 'days' | 'months' | 'years',
}

const createPolicy = ({ renewal_policy, lifetime_unit, lifetime_value }: FormValues) => {
  const lifetime = moment.duration(lifetime_value, lifetime_unit);
  const payload = {
    mode: renewal_policy,
    certificate_lifetime: lifetime.toISOString(),
  };

  return fetch(
    'POST',
    qualifyUrl('/api/renewal_policy'),
    payload,
    false,
  );
};

const StyledForm = styled(Form)`
  > div:not(:last-child) {
    margin-bottom: 10px;
  }
`;

const MINIMUM_LIFETIME = moment.duration(2, 'hours');

const validateForm = (formValues: FormValues) => {
  const duration = moment.duration(formValues.lifetime_value, formValues.lifetime_unit);

  return duration.subtract(MINIMUM_LIFETIME).asMilliseconds() < 0
    ? { lifetime_value: `Must be at least ${MINIMUM_LIFETIME.humanize()}` }
    : {};
};

const unitOptions = [
  { label: 'Hour(s)', value: 'hours' },
  { label: 'Day(s)', value: 'days' },
  { label: 'Month(s)', value: 'months' },
  { label: 'Year(s)', value: 'years' },
];

const defaultFormValues = {
  renewal_policy: 'Automatic',
  lifetime_value: 30,
  lifetime_unit: 'days',
};

const RenewalPolicyConfiguration = () => {
  const queryClient = useQueryClient();

  const { mutateAsync: onCreateRenewalPolicy } = useMutation(createPolicy, {
    onSuccess: () => {
      UserNotification.success('Renewal policy created successfully');
      queryClient.invalidateQueries(RENEWAL_POLICY_QUERY_KEY);
    },
    onError: (error) => {
      UserNotification.error(`Renewal policy creation failed with error: ${error}`);
    },
  });

  const onSubmit = (formValues: FormValues) => onCreateRenewalPolicy(formValues).catch(() => {});

  return (
    <>
      <Title order={3}>Configure Renewal Policy</Title>
      <p>
        In this step you can configure if certificates which are close to expiration should be renewed automatically.<br />
        If you choose manual renewal, a system notification will show up when the expiration date is near, requiring you to confirm renewal.
      </p>
      <Space h="md" />
      <Formik initialValues={defaultFormValues} onSubmit={(formValues: FormValues) => onSubmit(formValues)} validate={validateForm}>
        {({ isSubmitting, isValid, setFieldValue, errors }) => (
          <StyledForm>
            <Field name="renewal_policy">
              {({ field: { value, name } }) => (
                <Select placeholder="Select Renewal Policy"
                        data={['Automatic', 'Manual']}
                        required
                        value={value}
                        onChange={(newPolicy) => setFieldValue(name, newPolicy)}
                        label="Renewal Policy" />
              )}
            </Field>
            <Input.Label required>Certificate lifetime</Input.Label>
            <Group>
              <Field name="lifetime_value">
                {({ field: { name, value } }) => (
                  <NumberInput value={value}
                               onChange={(newValue) => setFieldValue(name, newValue)}
                               required
                               placeholder="Enter lifetime"
                               step={1} />
                )}
              </Field>
              <Field name="lifetime_unit">
                {({ field: { name, value } }) => (
                  <Select placeholder="Select Unit"
                          data={unitOptions}
                          required
                          value={value}
                          onChange={(unit) => setFieldValue(name, unit)} />
                )}
              </Field>
            </Group>
            {errors?.lifetime_value && <Input.Error>{errors?.lifetime_value}</Input.Error>}
            <Button disabled={isSubmitting || !isValid} type="submit">
              {isSubmitting ? 'Creating policy...' : 'Create policy'}
            </Button>
          </StyledForm>
        )}
      </Formik>
    </>

  );
};

export default RenewalPolicyConfiguration;
