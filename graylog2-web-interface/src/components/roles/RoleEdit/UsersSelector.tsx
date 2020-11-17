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
import { PaginatedListType } from 'components/common/PaginatedItemOverview';
import UserOverview from 'logic/users/UserOverview';
import UsersDomain from 'domainActions/users/UsersDomain';
import { ThemeInterface } from 'theme';
import { Button } from 'components/graylog';
import { Select, ErrorAlert } from 'components/common';

type Props = {
  onSubmit: (user: Immutable.Set<UserOverview>) => Promise<PaginatedListType | null | undefined>,
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
  const [users, setUsers] = useState<Immutable.List<UserOverview>>(Immutable.List.of());
  const [options, setOptions] = useState([]);
  const [error, setError] = useState<string | undefined>();

  const _loadUsers = useCallback(() => {
    const getUnlimited = { page: 1, perPage: 0, query: '' };

    UsersDomain.loadUsersPaginated(getUnlimited)
      .then((paginatedUsers) => {
        const resultUsers = paginatedUsers.list
          .filter((u) => !u.roles.includes(role.name))
          .map((u) => ({ label: u.name, value: u.name }))
          .toArray();

        setOptions(resultUsers);
        setUsers(paginatedUsers.list);
      });
  }, [role]);

  const onUpdate = ({ user }: { user: string }, { resetForm }) => {
    if (!user) {
      return;
    }

    const newUsers = user.split(',');
    const userOverview: Immutable.Set<UserOverview> = Immutable.Set(compact(newUsers.map((newUser) => {
      return users.find((u) => u.username === newUser);
    })));

    if (!userOverview) {
      setError(`This is an exceptional error! Unable to find user with name ${user} in ${users.map((u) => u.username).join(', ')}`);

      return;
    }

    setError(undefined);
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
      <ErrorAlert onClose={setError} runtimeError>
        {error}
      </ErrorAlert>
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
                {Object.entries(errors).map(([fieldKey, value]: [string, unknown]) => (
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
