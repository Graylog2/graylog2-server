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
import { useContext } from 'react';
import type * as Immutable from 'immutable';
import type { FormikProps } from 'formik';
import { Formik, Form, Field } from 'formik';
import styled from 'styled-components';
import { Trans, useTranslation } from 'react-i18next';

import type Role from 'logic/roles/Role';
import { validateField, formHasErrors } from 'util/FormsUtils';
import { FormikFormGroup, Select, InputList, TimezoneSelect } from 'components/common';
import { Alert, Button, ButtonToolbar, Row, Col, Panel, Input } from 'components/bootstrap';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

import type { WizardFormValues } from './BackendWizardContext';
import BackendWizardContext from './BackendWizardContext';

export const STEP_KEY = 'user-synchronization';
// Form validation needs to include all input names
// to be able to associate backend validation errors with the form
export const FORM_VALIDATION = {
  defaultRoles: { required: true },
  defaultUserTimezone: {},
  userFullNameAttribute: { required: true },
  userNameAttribute: { required: true },
  emailAttributes: {},
  userSearchBase: { required: true },
  userSearchPattern: { required: true },
  userUniqueIdAttribute: {},
};

type Props = {
  formRef: React.Ref<FormikProps<WizardFormValues>>;
  help?: { [inputName: string]: React.ReactElement | string | null | undefined };
  excludedFields?: { [inputName: string]: boolean };
  roles: Immutable.List<Role>;
  onSubmit: () => void;
  onSubmitAll: () => Promise<void>;
  submitAllError: React.ReactNode | null | undefined;
  validateOnMount: boolean;
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
  const sendTelemetry = useSendTelemetry('directory-service');
  const { t } = useTranslation('authentication');

  const _onSubmitAll = (validateForm) => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.AUTHENTICATION.DIRECTORY_USER_SYNC_SAVE_CLICKED, {
      app_action_value: 'usersync-save',
    });

    validateForm().then((errors) => {
      if (!formHasErrors(errors)) {
        onSubmitAll();
      }
    });
  };

  const getInitalFormValues = (values: WizardFormValues) => ({
    ...values,
    ...(!excludedFields.emailAttributes && { emailAttributes: values.emailAttributes || [] }),
  });

  return (
    <Formik
      initialValues={getInitalFormValues(stepsState.formValues)}
      initialErrors={backendValidationErrors}
      innerRef={formRef}
      onSubmit={onSubmit}
      validateOnBlur={false}
      validateOnChange={false}
      validateOnMount={validateOnMount}>
      {({ isSubmitting, validateForm }) => (
        <Form className="form form-horizontal">
          <FormikFormGroup
            help={help.userSearchBase}
            label={t('user_sync.search_base_dn')}
            error={backendValidationErrors?.userSearchBase}
            name="userSearchBase"
            placeholder={t('user_sync.search_base_dn')}
            validate={validateField(FORM_VALIDATION.userSearchBase)}
          />

          <FormikFormGroup
            help={help.userSearchPattern}
            label={t('user_sync.search_pattern')}
            name="userSearchPattern"
            error={backendValidationErrors?.userSearchPattern}
            placeholder={t('user_sync.search_pattern')}
            validate={validateField(FORM_VALIDATION.userSearchPattern)}
          />

          <FormikFormGroup
            help={help.userNameAttribute}
            label={t('user_sync.name_attribute')}
            name="userNameAttribute"
            error={backendValidationErrors?.userNameAttribute}
            placeholder={t('user_sync.name_attribute')}
            validate={validateField(FORM_VALIDATION.userNameAttribute)}
          />

          {!excludedFields.emailAttributes && (
            <Field name="emailAttributes" validate={validateField(FORM_VALIDATION.emailAttributes)}>
              {({ field: { name, value, onChange }, meta: { error } }) => (
                <Input
                  bsStyle={error ? 'error' : undefined}
                  help={help.emailAttributes}
                  error={error ?? backendValidationErrors?.emailAttributes}
                  id="email-attributes-input"
                  label={t('user_sync.email_attributes')}
                  labelClassName="col-sm-3"
                  wrapperClassName="col-sm-9">
                  <StyledInputList
                    id="userEmailAttributes"
                    placeholder={t('user_sync.email_attributes')}
                    name={name}
                    values={value}
                    isClearable
                    onChange={onChange}
                  />
                </Input>
              )}
            </Field>
          )}
          <FormikFormGroup
            help={help.userFullNameAttribute}
            label={t('user_sync.full_name_attribute')}
            name="userFullNameAttribute"
            placeholder={t('user_sync.full_name_attribute')}
            error={backendValidationErrors?.userFullNameAttribute}
            validate={validateField(FORM_VALIDATION.userFullNameAttribute)}
          />

          {!excludedFields.userUniqueIdAttribute && (
            <FormikFormGroup
              help={help.userUniqueIdAttribute}
              label={t('user_sync.id_attribute')}
              name="userUniqueIdAttribute"
              placeholder={t('user_sync.id_attribute')}
              error={backendValidationErrors?.userUniqueIdAttribute}
              validate={validateField(FORM_VALIDATION.userUniqueIdAttribute)}
            />
          )}

          <Row>
            <Col sm={9} smOffset={3}>
              <Panel bsStyle="info">
                <Trans
                  t={t}
                  i18nKey="user_sync.default_assignments_hint"
                  values={{ serviceTitle: stepsState.authBackendMeta.serviceTitle }}
                />
              </Panel>
            </Col>
          </Row>

          <Field name="defaultRoles" validate={validateField(FORM_VALIDATION.defaultRoles)}>
            {({ field: { name, value, onChange, onBlur }, meta: { error } }) => (
              <Input
                bsStyle={error ? 'error' : undefined}
                help={help.defaultRoles}
                error={error ?? backendValidationErrors?.defaultRoles}
                id="default-roles-select"
                label={t('user_sync.default_roles')}
                labelClassName="col-sm-3"
                wrapperClassName="col-sm-9">
                <Select
                  multi
                  onBlur={onBlur}
                  onChange={(selectedRoles) => onChange({ target: { value: selectedRoles, name } })}
                  options={rolesOptions}
                  placeholder={t('user_sync.search_roles')}
                  value={value}
                />
              </Input>
            )}
          </Field>

          <Field name="defaultUserTimezone">
            {({ field: { name, value, onChange } }) => (
              <Input
                id="default-user-timezone-select"
                help={help.defaultUserTimezone}
                label={t('user_sync.default_user_time_zone')}
                labelClassName="col-sm-3"
                wrapperClassName="col-sm-9">
                <TimezoneSelect
                  value={value || t('user_sync.browser_time_zone')}
                  name="timezone"
                  onChange={(newValue) => onChange({ target: { name, value: newValue } })}
                />
              </Input>
            )}
          </Field>

          <Row>
            <Col sm={9} smOffset={3}>
              <Alert bsStyle="info">{t('user_sync.test_login_hint')}</Alert>
            </Col>
          </Row>

          {submitAllError}

          <ButtonToolbar className="pull-right">
            <Button disabled={isSubmitting} onClick={() => _onSubmitAll(validateForm)} type="button">
              {t('wizard.finish_and_save_identity_service')}
            </Button>
            <Button
              bsStyle="primary"
              disabled={isSubmitting}
              onClick={() => {
                sendTelemetry(TELEMETRY_EVENT_TYPE.AUTHENTICATION.DIRECTORY_NEXT_GROUP_SYNC_CLICKED, {
                  app_action_value: 'groupsync-button',
                });
              }}
              type="submit">
              {t('wizard.next_group_sync')}
            </Button>
          </ButtonToolbar>
        </Form>
      )}
    </Formik>
  );
};

export default UserSyncStep;
