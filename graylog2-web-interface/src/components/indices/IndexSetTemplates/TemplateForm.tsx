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

import type { IndexSetTemplate } from 'components/indices/IndexSetTemplates/types';
import { FormikInput, FormSubmit, InputOptionalInfo } from 'components/common';
import { Col } from 'components/bootstrap';

const StyledFormSubmit = styled(FormSubmit)`
  margin-top: 30px;
`;

type Props = {
  initialValues?: Omit<IndexSetTemplate, 'id'>,
  submitButtonText: string,
  submitLoadingText: string,
  onCancel: () => void,
  onSubmit: (template: IndexSetTemplate) => void
}

const validate = (formValues: IndexSetTemplate) => {
  const errors: { title?: string } = {};

  if (!formValues.title) {
    errors.title = 'Template title is required';
  }

  return errors;
};

const TemplateForm = ({ initialValues, submitButtonText, submitLoadingText, onCancel, onSubmit }: Props) => {
  const _onSubmit = (template: IndexSetTemplate) => {
    onSubmit(template);
  };

  return (
    <Col lg={8}>
      <Formik<Omit<IndexSetTemplate, 'id'>> initialValues={initialValues}
                                            onSubmit={_onSubmit}
                                            validate={validate}
                                            validateOnChange>
        {({ isSubmitting, isValid, isValidating }) => (
          <Form>
            <FormikInput name="title"
                         label="Template title"
                         id="index-set-template-title"
                         placeholder="Type a template title"
                         help="A descriptive title of the template"
                         required />
            <FormikInput name="description"
                         id="index-set-template-description"
                         placeholder="Type a template description"
                         label={<>Description <InputOptionalInfo /></>}
                         type="textarea"
                         help="Longer description for template"
                         rows={6} />
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
