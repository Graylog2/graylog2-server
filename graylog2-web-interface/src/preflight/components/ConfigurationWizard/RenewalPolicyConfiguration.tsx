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
import { useTranslation } from 'react-i18next';

import i18n from 'i18n';
import { Title, Space, Group, NumberInput, Input } from 'preflight/components/common';
import Button from 'components/bootstrap/Button';
import UserNotification from 'util/UserNotification';
import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import Select from 'preflight/components/common/Select';
import { QUERY_KEY as RENEWAL_POLICY_QUERY_KEY } from 'preflight/hooks/useRenewalPolicy';

type FormValues = {
  renewal_policy: 'Automatic' | 'Manual';
  lifetime_value: number;
  lifetime_unit: 'hours' | 'days' | 'months' | 'years';
};

const createPolicy = ({ renewal_policy, lifetime_unit, lifetime_value }: FormValues) => {
  const lifetime = moment.duration(lifetime_value, lifetime_unit);
  const payload = {
    mode: renewal_policy,
    certificate_lifetime: lifetime.toISOString(),
  };

  return fetch('POST', qualifyUrl('/api/renewal_policy'), payload, false);
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
    ? {
        lifetime_value: i18n.t('renewal_policy.min_lifetime', {
          ns: 'preflight',
          duration: MINIMUM_LIFETIME.humanize(),
        }),
      }
    : {};
};

// Labels are translation keys, translated where they are rendered. The values are sent to the server.
const unitOptions = [
  { label: 'renewal_policy.unit_hours', value: 'hours' },
  { label: 'renewal_policy.unit_days', value: 'days' },
  { label: 'renewal_policy.unit_months', value: 'months' },
  { label: 'renewal_policy.unit_years', value: 'years' },
];

const defaultFormValues = {
  renewal_policy: 'Automatic',
  lifetime_value: 30,
  lifetime_unit: 'days',
};

const RenewalPolicyConfiguration = () => {
  const { t } = useTranslation('preflight');
  const queryClient = useQueryClient();

  const { mutateAsync: onCreateRenewalPolicy } = useMutation({
    mutationFn: createPolicy,

    onSuccess: () => {
      UserNotification.success(t('renewal_policy.success'));
      queryClient.invalidateQueries({ queryKey: RENEWAL_POLICY_QUERY_KEY });
    },

    onError: (error) => {
      UserNotification.error(t('renewal_policy.error', { error: String(error) }));
    },
  });

  const onSubmit = (formValues: FormValues) => onCreateRenewalPolicy(formValues).catch(() => {});

  return (
    <>
      <Title order={3}>{t('renewal_policy.title')}</Title>
      <p>
        {t('renewal_policy.explanation_automatic')}
        <br />
        {t('renewal_policy.explanation_manual')}
      </p>
      <Space h="md" />
      <Formik
        initialValues={defaultFormValues}
        onSubmit={(formValues: FormValues) => onSubmit(formValues)}
        validate={validateForm}>
        {({ isSubmitting, isValid, setFieldValue, errors }) => (
          <StyledForm>
            <Field name="renewal_policy">
              {({ field: { value, name } }) => (
                <Select
                  placeholder={t('renewal_policy.select_policy')}
                  data={[
                    // The values are sent to the server.
                    { label: t('renewal_policy.automatic'), value: 'Automatic' },
                    { label: t('renewal_policy.manual'), value: 'Manual' },
                  ]}
                  required
                  value={value}
                  onChange={(newPolicy) => setFieldValue(name, newPolicy)}
                  label={t('renewal_policy.policy')}
                />
              )}
            </Field>
            <Input.Label required>{t('renewal_policy.certificate_lifetime')}</Input.Label>
            <Group>
              <Field name="lifetime_value">
                {({ field: { name, value } }) => (
                  <NumberInput
                    value={value}
                    onChange={(newValue) => setFieldValue(name, newValue)}
                    required
                    placeholder={t('renewal_policy.enter_lifetime')}
                    step={1}
                  />
                )}
              </Field>
              <Field name="lifetime_unit">
                {({ field: { name, value } }) => (
                  <Select
                    placeholder={t('renewal_policy.select_unit')}
                    data={unitOptions.map(({ label, value: unitValue }) => ({ label: t(label), value: unitValue }))}
                    required
                    value={value}
                    onChange={(unit) => setFieldValue(name, unit)}
                  />
                )}
              </Field>
            </Group>
            {errors?.lifetime_value && <Input.Error>{errors?.lifetime_value}</Input.Error>}
            <Button disabled={isSubmitting || !isValid} type="submit">
              {isSubmitting ? t('renewal_policy.creating') : t('renewal_policy.create')}
            </Button>
          </StyledForm>
        )}
      </Formik>
    </>
  );
};

export default RenewalPolicyConfiguration;
