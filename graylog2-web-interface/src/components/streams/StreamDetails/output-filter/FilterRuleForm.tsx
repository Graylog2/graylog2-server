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
import { Formik, Field } from 'formik';
import { useTranslation } from 'react-i18next';

import { BootstrapModalWrapper, Input, Modal } from 'components/bootstrap';
import { FormikInput, ModalSubmit } from 'components/common';
import { formHasErrors, getValueFromInput } from 'util/FormsUtils';
import type { StreamOutputFilterRule } from 'components/streams/StreamDetails/output-filter/Types';
import FilterRulesFields from 'components/streams/StreamDetails/output-filter/FilterRulesFields';

type StreamOutputFilterRuleValues = Partial<StreamOutputFilterRule>;
type Props = {
  title: string;
  filterRule: Partial<StreamOutputFilterRule>;
  onCancel: () => void;
  handleSubmit: (values: Partial<StreamOutputFilterRule>) => void;
  destinationType: string;
};

const FilterRuleForm = ({ title, filterRule, onCancel, handleSubmit, destinationType }: Props) => {
  const { t } = useTranslation('streams');
  const validate = (values: Partial<StreamOutputFilterRule>) => {
    const { title: currentTitle, rule } = values;
    let errors = {};

    if (!currentTitle) {
      errors = { ...errors, title: t('output_filter.form.title_required') };
    }

    if (!rule?.conditions || (rule?.conditions && rule.conditions.length <= 0)) {
      errors = { ...errors, rule: t('output_filter.form.condition_required') };
    }

    return errors;
  };

  return (
    <BootstrapModalWrapper showModal bsSize="lg" onHide={onCancel}>
      <Formik<StreamOutputFilterRuleValues>
        initialValues={{
          ...filterRule,
          destination_type: destinationType,
          ...(!filterRule?.id && {
            status: 'enabled',
            rule: {
              operator: 'AND',
              conditions: [],
              actions: [],
            },
          }),
        }}
        validate={validate}
        validateOnBlur
        validateOnChange
        onSubmit={() => {}}>
        {({ isSubmitting, values, isValid, errors, validateForm, setFieldValue }) => {
          const onSubmit = () => {
            validateForm().then((errorsList) => {
              if (!formHasErrors(errorsList as Record<string, string>)) {
                handleSubmit(values);
              }
            });
          };

          const onStatusChange = (event: React.ChangeEvent<HTMLInputElement>) => {
            const isChecked = getValueFromInput(event.target);
            setFieldValue('status', isChecked ? 'enabled' : 'disabled');
          };

          return (
            <>
              <Modal.Header>
                <Modal.Title>{title}</Modal.Title>
              </Modal.Header>
              <Modal.Body>
                <FormikInput
                  id="title"
                  name="title"
                  label={t('output_filter.form.title')}
                  help={t('output_filter.form.title_help')}
                  error={errors.title}
                  required
                />
                <FormikInput
                  id="description"
                  name="description"
                  label={t('output_filter.form.description')}
                  help={t('output_filter.form.description_help')}
                />
                <Field name="status">
                  {({ field: { name, value }, meta }) => (
                    <Input
                      id={name}
                      error={meta?.error}
                      label={t('output_filter.form.enabled')}
                      type="checkbox"
                      onChange={onStatusChange}
                      checked={value === 'enabled'}
                      value={value === 'enabled'}
                    />
                  )}
                </Field>
                <label htmlFor="rule_builder">{t('output_filter.form.rule_builder')}</label>
                {errors?.rule && <p className="text-danger">{errors.rule as React.ReactNode}</p>}
                <FilterRulesFields type="condition" />
                <Modal.Footer>
                  <ModalSubmit
                    isSubmitting={isSubmitting}
                    isAsyncSubmit
                    onSubmit={onSubmit}
                    onCancel={onCancel}
                    disabledSubmit={!isValid || values?.rule?.errors?.length > 0}
                    submitButtonText={values?.id ? t('output_filter.form.update') : t('output_filter.form.create')}
                    submitLoadingText={values?.id ? t('output_filter.form.updating') : t('output_filter.form.saving')}
                  />
                </Modal.Footer>
              </Modal.Body>
            </>
          );
        }}
      </Formik>
    </BootstrapModalWrapper>
  );
};

export default FilterRuleForm;
