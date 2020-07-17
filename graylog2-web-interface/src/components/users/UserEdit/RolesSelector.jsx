// @flow strict
import * as React from 'react';
import { useEffect, useState } from 'react';
import { Formik, Form, Field } from 'formik';
import styled, { type StyledComponent } from 'styled-components';

import { AuthzRolesActions, type PaginatedListType } from 'stores/roles/AuthzRolesStore';
import { type ThemeInterface } from 'theme';
import { Button } from 'components/graylog';
import { Select } from 'components/common';
import User from 'logic/users/User';

type Props = {
  onSubmit: ({ roles: string[] }) => Promise<void>,
  user: User,
};

const SubmitButton = styled(Button)`
  margin-left: 15px;
`;

const FormElements = styled.div`
  display: flex;
`;

const Errors: StyledComponent<{}, ThemeInterface, HTMLDivElement> = styled.div(({ theme }) => `
  width: 100%;
  margin-top: 3px;
  color: ${theme.colors.variant.danger};

  > * {
    margin-right: 5px;

    :last-child {
      margin-right: 0;
    }
  }
`);

const RoleSelectOption = styled.div`
  display: flex;
  align-items: center;
`;

const StyledSelect = styled(Select)`
  flex: 1;
`;

const _renderRoleOption = ({ label }: { label: string }) => (
  <RoleSelectOption>{label}</RoleSelectOption>
);

const _isRequired = (field) => (value) => (!value ? `The ${field} is required` : undefined);

const RolesSelector = ({ user, onSubmit }: Props) => {
  const [roles, setRoles] = useState([]);

  const onUpdate = ({ role }: { role: string }, { resetForm }) => {
    const userRoles = user.roles;
    const newRoles = userRoles.push(role).toJS();

    onSubmit({ roles: newRoles }).then(() => {
      resetForm();
    });
  };

  useEffect(() => {
    AuthzRolesActions.loadPaginated(1, 0, '')
      .then((response: PaginatedListType) => {
        const { list } = response;

        const resultRoles = list
          .filter((r) => !user.roles.includes(r.name))
          .toArray()
          .map((r) => ({ label: r.name, value: r.name }));
        setRoles(resultRoles);
      });
  }, [user]);

  return (
    <div>
      <Formik onSubmit={onUpdate}
              initialValues={{ role: undefined }}>
        {({ isSubmitting, isValid, errors }) => (
          <Form>
            <FormElements>
              <Field name="role" validate={_isRequired('role')}>
                {({ field: { name, value, onChange } }) => (
                  <StyledSelect inputProps={{ 'arial-label': 'Search for roles ' }}
                                onChange={(role) => {
                                  onChange({ target: { value: role, name } });
                                }}
                                optionRenderer={_renderRoleOption}
                                options={roles}
                                placeholder="Search for roles"
                                value={value} />
                )}
              </Field>
              <SubmitButton bsStyle="success"
                            disabled={isSubmitting || !isValid}
                            title="Add Collaborator"
                            type="submit">
                Add Role
              </SubmitButton>
            </FormElements>
            {errors && (
              <Errors>
                {Object.entries(errors).map(([fieldKey, value]: [string, mixed]) => (
                  <span key={fieldKey}>{String(value)}.</span>
                ))}
              </Errors>
            )}
          </Form>
        )}
      </Formik>
    </div>
  );
};

export default RolesSelector;
