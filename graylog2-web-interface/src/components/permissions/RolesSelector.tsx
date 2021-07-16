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
import * as Immutable from 'immutable';
import { compact } from 'lodash';
import PropTypes from 'prop-types';
import { useEffect, useState } from 'react';
import styled from 'styled-components';

import type { PaginatedRoles } from 'actions/roles/AuthzRolesActions';
import AuthzRolesDomain from 'domainActions/roles/AuthzRolesDomain';
import Role from 'logic/roles/Role';
import { Button } from 'components/graylog';
import { Select, Spinner, ErrorAlert } from 'components/common';

type Props = {
  assignedRolesIds: Immutable.Set<string>,
  identifier: (role: Role) => string,
  multi?: boolean,
  placeholder?: string,
  onSubmit: (role: Immutable.Set<Role>) => Promise<void>,
};

const SubmitButton = styled(Button)`
  margin-left: 15px;
`;

const FormElements = styled.div`
  display: flex;
`;

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

const _options = (roles, assignedRolesIds, identifier) => roles
  .filter((role) => !assignedRolesIds.includes(identifier(role)))
  .toArray()
  .map((r) => ({ label: r.name, value: r.name, role: r }));

const _assignRole = (selectedRoleNames, roles, onSubmit, setSelectedRoleNames, setIsSubmitting, setError) => {
  if (!selectedRoleNames) {
    return;
  }

  const selectedRoleNamess = selectedRoleNames.split(',');

  const selectedRoles = Immutable.Set(compact(selectedRoleNamess.map((selection) => {
    return roles.find((r) => r.name === selection);
  })));

  if (selectedRoles.size <= 0) {
    setError(`Role assignment failed, because the roles ${selectedRoleNames ?? '(undefined)'} does not exist`);

    return;
  }

  setError();
  setIsSubmitting(true);

  onSubmit(selectedRoles).then(() => {
    setSelectedRoleNames();
    setIsSubmitting(false);
  });
};

const _loadRoles = (setPaginatedRoles) => {
  const getUnlimited = { page: 1, perPage: 0, query: '' };

  AuthzRolesDomain.loadRolesPaginated(getUnlimited).then(setPaginatedRoles);
};

const RolesSelector = ({ assignedRolesIds, onSubmit, identifier, multi, placeholder }: Props) => {
  const [paginatedRoles, setPaginatedRoles] = useState<PaginatedRoles | undefined>();
  const [selectedRoleNames, setSelectedRoleNames] = useState();
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | undefined>();

  useEffect(() => _loadRoles(setPaginatedRoles), []);

  if (!paginatedRoles) {
    return <Spinner />;
  }

  const _onSubmit = (newSelectedRoleNames) => _assignRole(newSelectedRoleNames, paginatedRoles.list, onSubmit, setSelectedRoleNames, setIsSubmitting, setError);

  const _onRoleSelect = (newSelectedRoleNames) => {
    if (multi) {
      setSelectedRoleNames(newSelectedRoleNames);
    } else {
      _assignRole(newSelectedRoleNames, paginatedRoles.list, onSubmit, setSelectedRoleNames, setIsSubmitting, setError);
    }
  };

  const options = _options(paginatedRoles.list, assignedRolesIds, identifier);

  return (
    <div>
      <FormElements>
        <StyledSelect inputProps={{ 'aria-label': 'Search for roles' }}
                      onChange={_onRoleSelect}
                      optionRenderer={_renderRoleOption}
                      options={options}
                      placeholder={placeholder}
                      multi={multi}
                      value={multi ? selectedRoleNames : null} />
        {multi && (
          <SubmitButton bsStyle="success"
                        onClick={() => _onSubmit(selectedRoleNames)}
                        disabled={isSubmitting || !selectedRoleNames}
                        title="Assign Role"
                        type="button">
            Assign Role
          </SubmitButton>
        )}
      </FormElements>
      <ErrorAlert runtimeError onClose={setError}>
        {error}
      </ErrorAlert>
    </div>
  );
};

RolesSelector.defaultProps = {
  identifier: (role) => role.id,
  placeholder: 'Search for roles',
  multi: false,
};

RolesSelector.propTypes = {
  identifier: PropTypes.func,
  multi: PropTypes.bool,
  placeholder: PropTypes.string,
  onSubmit: PropTypes.func.isRequired,
};

export default RolesSelector;
