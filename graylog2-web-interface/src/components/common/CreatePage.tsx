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
  submitError: string | null;
  children: React.ReactNode;
};

const CreatePageContent = ({
  entityName,
  onCancel,
  description = undefined,
  documentationLink = undefined,
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
          <Form>
            {children}
            {submitError && (
              <Alert bsStyle="danger" title="Failed to save">
                {submitError}
              </Alert>
            )}
            <FormSubmit
              submitButtonText={`Create ${entityNameLower}`}
              submitLoadingText={`Creating ${entityNameLower}...`}
              isSubmitting={isSubmitting}
              isAsyncSubmit
              disabledSubmit={!isValid || isValidating}
              onCancel={onCancel}
            />
          </Form>
        </Col>
      </Row>
    </DocumentTitle>
  );
};

type Props<TValues extends object> = {
  entityName: string;
  overviewRoute: string;
  detailsRoute: (id: string) => string;
  initialValues: TValues;
  onSubmit: (values: TValues) => Promise<{ id: string }>;
  validate?: (values: TValues) => FormikErrors<TValues> | Promise<FormikErrors<TValues>>;
  description?: React.ReactNode;
  documentationLink?: { title: string; path: string };
  children: React.ReactNode;
};

const CreatePage = <TValues extends object>({
  entityName,
  overviewRoute,
  detailsRoute,
  initialValues,
  onSubmit,
  validate = undefined,
  description = undefined,
  documentationLink = undefined,
  children,
}: Props<TValues>) => {
  const [submitError, setSubmitError] = useState<string | null>(null);
  const history = useHistory();

  const handleCancel = () => (window.history.length > 1 ? history.goBack() : history.push(overviewRoute));

  const handleSubmit = async (values: TValues) => {
    setSubmitError(null);

    try {
      const { id } = await onSubmit(values);
      history.push(detailsRoute(id));
    } catch (error) {
      setSubmitError(error?.message ?? 'An error occurred. Please try again.');
    }
  };

  return (
    <Formik<TValues> initialValues={initialValues} onSubmit={handleSubmit} validate={validate} validateOnBlur={false}>
      <CreatePageContent
        entityName={entityName}
        onCancel={handleCancel}
        description={description}
        documentationLink={documentationLink}
        submitError={submitError}>
        {children}
      </CreatePageContent>
    </Formik>
  );
};

export default CreatePage;
