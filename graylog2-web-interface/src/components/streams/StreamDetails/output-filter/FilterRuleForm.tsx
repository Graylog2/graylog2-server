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

import { BootstrapModalWrapper, Input, Modal } from 'components/bootstrap';
import { FormikInput, ModalSubmit } from 'components/common';
import { formHasErrors, getValueFromInput } from 'util/FormsUtils';
import type { StreamOutputFilterRule } from 'components/streams/StreamDetails/output-filter/Types';
import FilterRulesFields from 'components/streams/StreamDetails/output-filter/FilterRulesFields';

type StreamOutputFilterRuleValues = Partial<StreamOutputFilterRule>;
type Props = {
  title: string,
  filterRule: Partial<StreamOutputFilterRule>,
  onCancel: () => void,
  handleSubmit: (values: Partial<StreamOutputFilterRule>) => void,
  destinationType: string,
};

const FilterRuleForm = ({ title, filterRule, onCancel, handleSubmit, destinationType }: Props) => {
  const validate = (values: Partial<StreamOutputFilterRule>) => {
    const { title: currentTitle, rule } = values;
    let errors = {};

    if (!currentTitle) {
      errors = { ...errors, title: 'The "Title" field is required.' };
    }

    if (!rule?.conditions || (rule?.conditions && rule.conditions.length <= 0)) {
      errors = { ...errors, rule: 'Rule needs to contain at least one condition.' };
    }

    return errors;
  };

  return (
    <BootstrapModalWrapper showModal
                           bsSize="lg"
                           role="alertdialog"
                           onHide={onCancel}>
      <Formik<StreamOutputFilterRuleValues> initialValues={{
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
              if (!formHasErrors(errorsList)) {
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
              <Modal.Header closeButton>
                <Modal.Title>{title}</Modal.Title>
              </Modal.Header>
              <Modal.Body>
                <FormikInput id="title"
                             name="title"
                             label="Title"
                             help="Rule title"
                             error={errors.title}
                             required />
                <FormikInput id="description"
                             name="description"
                             label="Description"
                             help="Rule description" />
                <Field name="status">
                  {({ field: { name, value }, meta }) => (
                    <Input id={name}
                           error={meta?.error}
                           label="Enabled"
                           type="checkbox"
                           onChange={onStatusChange}
                           checked={value === 'enabled'}
                           value={value === 'enabled'} />
                  )}
                </Field>
                <label htmlFor="rule_builder">Rule Builder</label>
                {errors?.rule && (<p className="text-danger">{errors.rule as React.ReactNode}</p>)}
                <FilterRulesFields type="condition" />
                <Modal.Footer>
                  <ModalSubmit isSubmitting={isSubmitting}
                               isAsyncSubmit
                               onSubmit={onSubmit}
                               onCancel={onCancel}
                               disabledSubmit={!isValid || values?.rule?.errors?.length > 0}
                               submitButtonText={values?.id ? 'Update' : 'Create'}
                               submitLoadingText={values?.id ? 'Updating filter' : 'Saving filter'} />
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
