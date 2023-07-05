import * as React from 'react';
import { Formik, Form, Field } from 'formik';
import styled from 'styled-components';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import moment from 'moment';

import { Title, Space, Button, Group, NumberInput } from 'preflight/components/common';
import UserNotification from 'preflight/util/UserNotification';
import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import Select from 'preflight/components/common/Select';

const RENEWAL_POLICY_QUERY_KEY = ['data_node_renewal_policy'];

const createPolicy = (caData: FormValues) => fetch(
  'POST',
  qualifyUrl('/api/renewal_policy/create'),
  caData,
  false,
);

const StyledForm = styled(Form)`
  > div:not(:last-child) {
    margin-bottom: 10px;
  }
`;

const MINIMUM_LIFETIME = moment.duration(2, 'hours');

const validateForm = (formValues: FormValues) => {
  const duration = moment.duration(formValues.lifetime_value, formValues.lifetime_unit);

  return duration.subtract(MINIMUM_LIFETIME).asMilliseconds() < 0
    ? {
      lifetime_value: `Must be at least ${MINIMUM_LIFETIME.humanize()}`,
    }
    : {};
};

type FormValues = {
  renewal_policy: 'Automatic' | 'Manual',
  lifetime_value: number,
  lifetime_unit: 'hours' | 'days' | 'months' | 'years',
}
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

  const { mutate: onSubmit } = useMutation(createPolicy, {
    onSuccess: () => {
      UserNotification.success('CA created successfully');
      queryClient.invalidateQueries(RENEWAL_POLICY_QUERY_KEY);
    },
    onError: (error) => {
      UserNotification.error(`CA creation failed with error: ${error}`);
    },
  });

  return (
    <>
      <Title order={3}>Configure Renewal Policy</Title>
      <p>
        In this step you can configure if certificates which are close to expiration should be renewed automatically or manually.<br />
        If you choose manual renewal, a system notification will show up when the expiration date is near, requiring you to confirm renewal.
      </p>
      <Space h="md" />
      <Formik initialValues={defaultFormValues} onSubmit={(formValues: FormValues) => onSubmit(formValues)} validate={validateForm}>
        {({ isSubmitting, isValid, setFieldValue }) => (
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
            <Group>
              <Field name="lifetime_value">
                {({ field: { name, value }, meta: { error } }) => (
                  <NumberInput value={value}
                               onChange={(newValue) => setFieldValue(name, newValue)}
                               label="Certificate lifetime"
                               required
                               placeholder="Enter lifetime"
                               error={error}
                               step={1} />
                )}
              </Field>
              <Field name="lifetime_unit">
                {({ field: { name, value } }) => (
                  <Select placeholder="Select Unit"
                          data={unitOptions}
                          required
                          label="Unit"
                          value={value}
                          onChange={(unit) => setFieldValue(name, unit)} />
                )}
              </Field>
            </Group>
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
