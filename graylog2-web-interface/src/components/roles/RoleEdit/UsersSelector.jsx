// @flow strict
import * as React from 'react';
import { useEffect, useState, useCallback } from 'react';
import { Formik, Form, Field } from 'formik';
import styled, { css } from 'styled-components';
import type { StyledComponent } from 'styled-components';
import { compact } from 'lodash';
import * as Immutable from 'immutable';

import { AuthzRolesActions } from 'stores/roles/AuthzRolesStore';
import Role from 'logic/roles/Role';
import { type PaginatedListType } from 'components/common/PaginatedItemOverview';
import UserOverview from 'logic/users/UserOverview';
import UsersDomain from 'domainActions/users/UsersDomain';
import { type ThemeInterface } from 'theme';
import { Button } from 'components/graylog';
import { Select } from 'components/common';

type Props = {
  onSubmit: (user: Immutable.Set<UserOverview>) => Promise<?PaginatedListType>,
  role: Role,
};

const SubmitButton = styled(Button)`
  margin-left: 15px;
`;

const FormElements = styled.div`
  display: flex;
`;

const Errors: StyledComponent<{}, ThemeInterface, HTMLDivElement> = styled.div(({ theme }) => css`
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

const SelectOption = styled.div`
  display: flex;
  align-items: center;
`;

const StyledSelect = styled(Select)`
  flex: 1;
`;

const _renderOption = ({ label }: { label: string }) => (
  <SelectOption>{label}</SelectOption>
);

const _isRequired = (field) => (value) => (!value ? `The ${field} is required` : undefined);

const UsersSelector = ({ role, onSubmit }: Props) => {
  const [users, setUsers] = useState([]);
  const [options, setOptions] = useState([]);

  const _loadUsers = useCallback(() => {
    const getUnlimited = [1, 0, ''];

    UsersDomain.loadUsersPaginated(...getUnlimited)
      .then((newPaginatedUsers) => {
        const resultUsers = newPaginatedUsers.list
          .filter((u) => !u.roles.includes(role.name))
          .map((u) => ({ label: u.name, value: u.name }))
          .toArray();

        setOptions(resultUsers);
        setUsers(newPaginatedUsers.list);
      });
  }, [role]);

  const onUpdate = ({ user }: { user: string }, { resetForm }) => {
    if (!user) {
      return;
    }

    const newUsers = user.split(',');
    const userOverview = Immutable.Set(compact(newUsers.map((newUser) => {
      return users.find((u) => u.username === newUser);
    })));

    if (!userOverview) {
      throw new Error(`Unable to find user with name ${user} in ${users.map((u) => u.username).join(', ')}`);
    }

    onSubmit(userOverview).then(() => { resetForm(); });
  };

  useEffect(() => {
    _loadUsers();

    const unlistenAddMember = AuthzRolesActions.addMembers.completed.listen(_loadUsers);
    const unlistenRemoveMember = AuthzRolesActions.removeMember.completed.listen(_loadUsers);

    return () => {
      unlistenRemoveMember();
      unlistenAddMember();
    };
  }, [role, _loadUsers]);

  return (
    <div>
      <Formik onSubmit={onUpdate}
              initialValues={{ user: undefined }}>
        {({ isSubmitting, isValid, errors }) => (
          <Form>
            <FormElements>
              <Field name="user" validate={_isRequired('user')}>
                {({ field: { name, value, onChange } }) => (
                  <StyledSelect inputProps={{ 'aria-label': 'Search for users' }}
                                onChange={(user) => {
                                  onChange({ target: { value: user, name } });
                                }}
                                optionRenderer={_renderOption}
                                multi
                                options={options}
                                placeholder="Search for users"
                                value={value} />
                )}
              </Field>
              <SubmitButton bsStyle="success"
                            disabled={isSubmitting || !isValid}
                            title="Assign User"
                            type="submit">
                Assign User
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

export default UsersSelector;
