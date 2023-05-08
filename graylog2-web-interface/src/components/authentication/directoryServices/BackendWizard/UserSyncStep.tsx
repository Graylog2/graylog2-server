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
import type * as Immutable from 'immutable';
import { useContext } from 'react';
import type { FormikProps } from 'formik';
import { Formik, Form, Field } from 'formik';
import styled from 'styled-components';

import type Role from 'logic/roles/Role';
import { validateField, formHasErrors } from 'util/FormsUtils';
import { Icon, FormikFormGroup, Select, InputList } from 'components/common';
import { Alert, Button, ButtonToolbar, Row, Col, Panel, Input } from 'components/bootstrap';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';

import type { WizardFormValues } from './BackendWizardContext';
import BackendWizardContext from './BackendWizardContext';

export const STEP_KEY = 'user-synchronization';
// Form validation needs to include all input names
// to be able to associate backend validation errors with the form
export const FORM_VALIDATION = {
  defaultRoles: { required: true },
  userFullNameAttribute: { required: true },
  userNameAttribute: { required: true },
  emailAttributes: {},
  userSearchBase: { required: true },
  userSearchPattern: { required: true },
  userUniqueIdAttribute: {},
};

type Props = {
  formRef: React.Ref<FormikProps<WizardFormValues>>,
  help: { [inputName: string]: React.ReactElement | string | null | undefined },
  excludedFields: { [inputName: string]: boolean },
  roles: Immutable.List<Role>,
  onSubmit: () => void,
  onSubmitAll: () => Promise<void>,
  submitAllError: React.ReactNode | null | undefined,
  validateOnMount: boolean,
};
const StyledInputList = styled(InputList)`
  margin: auto 15px;
`;

const UserSyncStep = ({
  help = {},
  excludedFields = {},
  formRef,
  onSubmit,
  onSubmitAll,
  submitAllError,
  validateOnMount,
  roles,
}: Props) => {
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const { setStepsState, ...stepsState } = useContext(BackendWizardContext);
  const { backendValidationErrors } = stepsState;
  const rolesOptions = roles.map((role) => ({ label: role.name, value: role.id })).toArray();

  const sendTelemetry = useSendTelemetry();

  const _onSubmitAll = (validateForm) => {
    sendTelemetry('click', {
      app_pathname: 'authentication',
      app_section: 'directory-service',
      app_action_value: 'usersync-save',
    });

    validateForm().then((errors) => {
      if (!formHasErrors(errors)) {
        onSubmitAll();
      }
    });
  };

  const getInitalFormValues = (values: WizardFormValues) => {
    return { ...values, ...(!excludedFields.emailAttributes && { emailAttributes: values.emailAttributes || [] }) };
  };

  return (
    <Formik initialValues={getInitalFormValues(stepsState.formValues)}
            initialErrors={backendValidationErrors}
            innerRef={formRef}
            onSubmit={onSubmit}
            validateOnBlur={false}
            validateOnChange={false}
            validateOnMount={validateOnMount}>
      {({ isSubmitting, validateForm }) => (
        <Form className="form form-horizontal">
          <FormikFormGroup help={help.userSearchBase}
                           label="Search Base DN"
                           error={backendValidationErrors?.userSearchBase}
                           name="userSearchBase"
                           placeholder="Search Base DN"
                           validate={validateField(FORM_VALIDATION.userSearchBase)} />

          <FormikFormGroup help={help.userSearchPattern}
                           label="Search Pattern"
                           name="userSearchPattern"
                           error={backendValidationErrors?.userSearchPattern}
                           placeholder="Search Pattern"
                           validate={validateField(FORM_VALIDATION.userSearchPattern)} />

          <FormikFormGroup help={help.userNameAttribute}
                           label="Name Attribute"
                           name="userNameAttribute"
                           error={backendValidationErrors?.userNameAttribute}
                           placeholder="Name Attribute"
                           validate={validateField(FORM_VALIDATION.userNameAttribute)} />

          {!excludedFields.emailAttributes && (
            <Field name="emailAttributes" validate={validateField(FORM_VALIDATION.emailAttributes)}>
              {({ field: { name, value, onChange }, meta: { error } }) => (
                <Input bsStyle={error ? 'error' : undefined}
                       help={help.emailAttributes}
                       error={error ?? backendValidationErrors?.emailAttributes}
                       id="email-attributes-input"
                       label="Email Attributes"
                       labelClassName="col-sm-3"
                       wrapperClassName="col-sm-9">
                  <StyledInputList id="userEmailAttributes"
                                   placeholder="Email Attributes"
                                   name={name}
                                   values={value}
                                   isClearable
                                   onChange={onChange} />
                </Input>
              )}
            </Field>
          )}
          <FormikFormGroup help={help.userFullNameAttribute}
                           label="Full Name Attribute"
                           name="userFullNameAttribute"
                           placeholder="Full Name Attribute"
                           error={backendValidationErrors?.userFullNameAttribute}
                           validate={validateField(FORM_VALIDATION.userFullNameAttribute)} />

          {!excludedFields.userUniqueIdAttribute && (
            <FormikFormGroup help={help.userUniqueIdAttribute}
                             label="ID Attribute"
                             name="userUniqueIdAttribute"
                             placeholder="ID Attribute"
                             error={backendValidationErrors?.userUniqueIdAttribute}
                             validate={validateField(FORM_VALIDATION.userUniqueIdAttribute)} />
          )}

          <Row>
            <Col sm={9} smOffset={3}>
              <Panel bsStyle="info">
                Changing the static role assignment will only affect new users created
                via {stepsState.authBackendMeta.serviceTitle}!
                Existing user accounts will be updated on their next login, or if you edit their roles manually.
              </Panel>
            </Col>
          </Row>

          <Field name="defaultRoles" validate={validateField(FORM_VALIDATION.defaultRoles)}>
            {({ field: { name, value, onChange, onBlur }, meta: { error } }) => (
              <Input bsStyle={error ? 'error' : undefined}
                     help={help.defaultRoles}
                     error={error ?? backendValidationErrors?.defaultRoles}
                     id="default-roles-select"
                     label="Default Roles"
                     labelClassName="col-sm-3"
                     wrapperClassName="col-sm-9">
                <Select inputProps={{ 'aria-label': 'Search for roles' }}
                        multi
                        onBlur={onBlur}
                        onChange={(selectedRoles) => onChange({ target: { value: selectedRoles, name } })}
                        options={rolesOptions}
                        placeholder="Search for roles"
                        value={value} />
              </Input>
            )}
          </Field>

          <Row>
            <Col sm={9} smOffset={3}>
              <Alert bsStyle="info">
                <Icon name="info-circle" />{' '}
                We recommend you test your user login in the sidebar panel to verify your settings.
              </Alert>
            </Col>
          </Row>

          {submitAllError}

          <ButtonToolbar className="pull-right">
            <Button disabled={isSubmitting}
                    onClick={() => _onSubmitAll(validateForm)}
                    type="button">
              Finish & Save Identity Service
            </Button>
            <Button bsStyle="success"
                    disabled={isSubmitting}
                    onClick={() => {
                      sendTelemetry('click', {
                        app_pathname: 'authentication',
                        app_section: 'directory-service',
                        app_action_value: 'groupsync-button',
                      });
                    }}
                    type="submit">
              Next: Group Synchronization
            </Button>
          </ButtonToolbar>
        </Form>
      )}
    </Formik>
  );
};

export default UserSyncStep;
