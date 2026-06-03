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
import { useState } from 'react';
import type { FormikErrors } from 'formik';
import { Formik, Form, useFormikContext } from 'formik';

import useHistory from 'routing/useHistory';
import { Alert, Col, Row } from 'components/bootstrap';
import DocumentTitle from 'components/common/DocumentTitle';
import FormSubmit from 'components/common/FormSubmit';
import PageHeader from 'components/common/PageHeader';

type ContentProps = {
  entityName: string;
  onCancel: () => void;
  description?: React.ReactNode;
  documentationLink?: { title: string; path: string };
  disabledSubmit?: boolean;
  submitError: string | null;
  children: React.ReactNode;
};

const CreatePageContent = ({
  entityName,
  onCancel,
  description = undefined,
  documentationLink = undefined,
  disabledSubmit = false,
  submitError,
  children,
}: ContentProps) => {
  const { isSubmitting, isValidating, isValid } = useFormikContext();
  const title = `Create ${entityName}`;
  const entityNameLower = entityName.toLowerCase();

  return (
    <DocumentTitle title={title}>
      <PageHeader title={title} documentationLink={documentationLink}>
        {description && <span>{description}</span>}
      </PageHeader>
      <Row className="content">
        <Col lg={8}>
          <Form className="form form-horizontal">
            {children}
            {submitError && (
              <Row>
                <Col xs={9} xsOffset={3}>
                  <Alert bsStyle="danger" title="Failed to save">
                    {submitError}
                  </Alert>
                </Col>
              </Row>
            )}
            <Row>
              <Col md={9} mdOffset={3}>
                <FormSubmit
                  submitButtonText={`Create ${entityNameLower}`}
                  submitLoadingText={`Creating ${entityNameLower}...`}
                  isSubmitting={isSubmitting}
                  isAsyncSubmit
                  disabledSubmit={!isValid || isValidating || disabledSubmit}
                  onCancel={onCancel}
                />
              </Col>
            </Row>
          </Form>
        </Col>
      </Row>
    </DocumentTitle>
  );
};

type Props<TValues extends object> = {
  entityName: string;
  overviewRoute: string;
  initialValues: TValues;
  onSubmit: (values: TValues) => Promise<void>;
  validate?: (values: TValues) => FormikErrors<TValues> | Promise<FormikErrors<TValues>>;
  description?: React.ReactNode;
  documentationLink?: { title: string; path: string };
  disabledSubmit?: boolean;
  children: React.ReactNode;
};

const CreatePage = <TValues extends object>({
  entityName,
  overviewRoute,
  initialValues,
  onSubmit,
  validate = undefined,
  description = undefined,
  documentationLink = undefined,
  disabledSubmit = false,
  children,
}: Props<TValues>) => {
  const [submitError, setSubmitError] = useState<string | null>(null);
  const history = useHistory();

  const handleCancel = () => history.push(overviewRoute);

  const handleSubmit = async (values: TValues) => {
    setSubmitError(null);

    try {
      await onSubmit(values);
      history.push(overviewRoute);
    } catch (error) {
      setSubmitError(error?.message ?? 'An error occurred. Please try again.');
    }
  };

  return (
    <Formik<TValues>
      initialValues={initialValues}
      onSubmit={handleSubmit}
      validate={validate}
      validateOnBlur={false}>
      <CreatePageContent
        entityName={entityName}
        onCancel={handleCancel}
        description={description}
        documentationLink={documentationLink}
        disabledSubmit={disabledSubmit}
        submitError={submitError}>
        {children}
      </CreatePageContent>
    </Formik>
  );
};

export default CreatePage;
