// @flow strict
import * as React from 'react';
import * as Immutable from 'immutable';
import { useContext } from 'react';
import { Formik, Form, Field } from 'formik';

import Role from 'logic/roles/Role';
import { validateField, formHasErrors } from 'util/FormsUtils';
import { Alert, Button, ButtonToolbar, Row, Col, Panel } from 'components/graylog';
import { Icon, FormikFormGroup, Select } from 'components/common';
import { Input } from 'components/bootstrap';

import BackendWizardContext from './BackendWizardContext';

export type StepKeyType = 'user-synchronization';
export const STEP_KEY: StepKeyType = 'user-synchronization';
export const FORM_VALIDATION = {
  defaultRoles: { required: true },
  userFullNameAttribute: { required: true },
  userNameAttribute: { required: true },
  userSearchBase: { required: true },
  userSearchPattern: { required: true },
};

type Props = {
  formRef: React.Ref<typeof Formik>,
  help: { [inputName: string]: ?React.Node },
  excludedFields: { [inputName: string]: boolean },
  roles: Immutable.List<Role>,
  onSubmit: () => void,
  onSubmitAll: () => Promise<void>,
  submitAllError: ?React.Node,
  validateOnMount: boolean,
};

const UserSyncStep = ({ help = {}, excludedFields = {}, formRef, onSubmit, onSubmitAll, submitAllError, validateOnMount, roles }: Props) => {
  const { setStepsState, ...stepsState } = useContext(BackendWizardContext);
  const rolesOptions = roles.map((role) => ({ label: role.name, value: role.id })).toArray();

  const _onSubmitAll = (validateForm) => {
    validateForm().then((errors) => {
      if (!formHasErrors(errors)) {
        onSubmitAll();
      }
    });
  };

  return (
    // $FlowFixMe innerRef works as expected
    <Formik initialValues={stepsState.formValues}
            innerRef={formRef}
            onSubmit={onSubmit}
            validateOnBlur={false}
            validateOnChange={false}
            validateOnMount={validateOnMount}>
      {({ isSubmitting, validateForm }) => (
        <Form className="form form-horizontal">
          <FormikFormGroup help={help.userSearchBase}
                           label="Search Base DN"
                           name="userSearchBase"
                           placeholder="Search Base DN"
                           validate={validateField(FORM_VALIDATION.userSearchBase)} />

          <FormikFormGroup help={help.userSearchPattern}
                           label="Search Pattern"
                           name="userSearchPattern"
                           placeholder="Search Pattern"
                           validate={validateField(FORM_VALIDATION.userSearchPattern)} />

          <FormikFormGroup help={help.userNameAttribute}
                           label="Name Attribute"
                           name="userNameAttribute"
                           placeholder="Name Attribute"
                           validate={validateField(FORM_VALIDATION.userNameAttribute)} />

          <FormikFormGroup help={help.userFullNameAttribute}
                           label="Full Name Attribute"
                           name="userFullNameAttribute"
                           placeholder="Full Name Attribute"
                           validate={validateField(FORM_VALIDATION.userFullNameAttribute)} />

          {!excludedFields.userUniqueIdAttribute && (
            <FormikFormGroup help={help.userUniqueIdAttribute}
                             label="ID Attribute"
                             name="userUniqueIdAttribute"
                             placeholder="ID Attribute"
                             validate={validateField(FORM_VALIDATION.userFullNameAttribute)} />
          )}

          <Row>
            <Col sm={9} smOffset={3}>
              <Panel bsStyle="info">
                Changing the static role assignment will only affect to new users created via LDAP/LDAP!<br />
                Existing user accounts will be updated on their next login, or if you edit their roles manually.
              </Panel>
            </Col>
          </Row>

          <Field name="defaultRoles" validate={validateField(FORM_VALIDATION.defaultRoles)}>
            {({ field: { name, value, onChange, onBlur }, meta: { error } }) => (
              <Input bsStyle={error ? 'error' : undefined}
                     help={help.defaultRoles}
                     error={error}
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
            <Button bsStyle="primary"
                    disabled={isSubmitting}
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
