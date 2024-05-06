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

import { styled } from 'styled-components';
import React from 'react';
import { Formik, Form } from 'formik';
import { PluginStore } from 'graylog-web-plugin/plugin';

import type { IndexSetTemplateFormValues, IndexSetTemplate } from 'components/indices/IndexSetTemplates/types';
import { FormikInput, FormSubmit, InputOptionalInfo, TimeUnitInput } from 'components/common';
import { prepareDataTieringConfig, prepareDataTieringInitialValues, DataTieringConfiguration } from 'components/indices/data-tiering';
import { Col } from 'components/bootstrap';

const TIME_UNITS = ['SECONDS', 'MINUTES'];

const StyledFormSubmit = styled(FormSubmit)`
  margin-top: 30px;
`;

type Props = {
  initialValues?: IndexSetTemplate,
  submitButtonText: string,
  submitLoadingText: string,
  onCancel: () => void,
  onSubmit: (template: IndexSetTemplate) => void
}

const validate = (formValues: IndexSetTemplateFormValues) => {
  const errors: {
    title?: string,
    index_set_config: {
      index_analyzer?: string,
      shards?: string,
      replicas?: string,
      index_optimization_max_num_segments?: string,
    }
  } = { index_set_config: {} };

  if (!formValues.title) {
    errors.title = 'Template title is required';
  }

  if (!formValues.index_set_config.index_analyzer) {
    errors.index_set_config.index_analyzer = 'Index analyzer is required';
  }

  if (!formValues.index_set_config.shards) {
    errors.index_set_config.shards = 'Shards is required';
  }

  if (!formValues.index_set_config.replicas) {
    errors.index_set_config.replicas = 'Replicas is required';
  }

  if (!formValues.index_set_config.index_optimization_max_num_segments) {
    errors.index_set_config.index_optimization_max_num_segments = 'Max. number of segments is required';
  }

  return errors;
};

const TemplateForm = ({ initialValues, submitButtonText, submitLoadingText, onCancel, onSubmit }: Props) => {
  const defaultValues = {
    index_set_config: {},
  };

  const handleSubmit = (values: IndexSetTemplateFormValues) => {
    let template = {};

    template = { ...values };

    if (values.index_set_config.data_tiering) {
      template = {
        ...values,
        index_set_config: {
          ...values.index_set_config,
          data_tiering: prepareDataTieringConfig(values.index_set_config.data_tiering, PluginStore),
        },
      };
    }

    onSubmit(template as unknown as IndexSetTemplate);
  };

  const prepareInitialValues = () => {
    const values = { ...defaultValues, ...initialValues };

    if (values.index_set_config.data_tiering) {
      return {
        ...values,
        index_set_config: {
          ...values.index_set_config,
          data_tiering: prepareDataTieringInitialValues(values.index_set_config.data_tiering),
        },
      };
    }

    return values as unknown as IndexSetTemplateFormValues;
  };

  return (
    <Col lg={8}>
      <Formik<IndexSetTemplateFormValues> initialValues={prepareInitialValues()}
                                          onSubmit={handleSubmit}
                                          validate={validate}
                                          validateOnChange>
        {({ isSubmitting, isValid, isValidating, setFieldValue, values }) => (
          <Form>
            <FormikInput name="title"
                         label="Template title"
                         id="index-set-template-title"
                         help="A descriptive title of the template"
                         required />
            <FormikInput name="description"
                         id="index-set-template-description"
                         label={<>Description <InputOptionalInfo /></>}
                         type="textarea"
                         help="Longer description for template"
                         rows={6} />
            <FormikInput name="index_set_config.index_analyzer"
                         label="Index Analyzer"
                         id="index-set-template-index-analyzer"
                         help="Index Analyzer" />
            <FormikInput name="index_set_config.shards"
                         label="Shards"
                         type="number"
                         id="index-set-template-shards"
                         help="Number of shards used per index in this index set" />
            <FormikInput name="index_set_config.replicas"
                         label="Replicas"
                         type="number"
                         id="index-set-template-replicas"
                         help="Number of replicas used per index in this index set" />
            <FormikInput name="index_set_config.index_optimization_max_num_segments"
                         label="Max. number of segments"
                         type="number"
                         id="index-set-template-index-optimization-max-num-segments"
                         help="Maximum number of segments per index after optimization (force merge)" />
            <FormikInput type="checkbox"
                         name="index_set_config.index_optimization_disabled"
                         id="index-set-template-index-optimization-disabled"
                         label="Disable index optimization after rotation"
                         help="Disable Elasticsearch index optimization (force merge) after rotation" />
            <TimeUnitInput label="Field type refresh interval"
                           update={(value, unit) => {
                             setFieldValue('index_set_config.field_type_refresh_interval', value);
                             setFieldValue('index_set_config.field_type_refresh_interval_unit', unit);
                           }}
                           value={values.index_set_config.field_type_refresh_interval}
                           unit={values.index_set_config.field_type_refresh_interval_unit}
                           enabled
                           hideCheckbox
                           units={TIME_UNITS} />

            <DataTieringConfiguration valuesPrefix="index_set_config" />

            <StyledFormSubmit submitButtonText={submitButtonText}
                              onCancel={onCancel}
                              disabledSubmit={isValidating || !isValid}
                              isSubmitting={isSubmitting}
                              submitLoadingText={submitLoadingText} />
          </Form>
        )}
      </Formik>
    </Col>
  );
};

TemplateForm.defaultProps = {
  initialValues: { title: '', description: '' },
};

export default TemplateForm;
