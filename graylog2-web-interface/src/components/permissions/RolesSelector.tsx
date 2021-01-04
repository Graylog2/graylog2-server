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

const _assignRole = (selectedRoleName, roles, onSubmit, setSelectedRoleName, setIsSubmitting, setError) => {
  if (!selectedRoleName) {
    return;
  }

  const selectedRoleNames = selectedRoleName.split(',');

  const selectedRoles = Immutable.Set(compact(selectedRoleNames.map((selection) => {
    return roles.find((r) => r.name === selection);
  })));

  if (selectedRoles.size <= 0) {
    setError(`Role assignment failed, because the roles ${selectedRoleName ?? '(undefined)'} does not exist`);

    return;
  }

  setError();
  setIsSubmitting(true);

  onSubmit(selectedRoles).then(() => {
    setSelectedRoleName();
    setIsSubmitting(false);
  });
};

const _loadRoles = (setPaginatedRoles) => {
  const getUnlimited = { page: 1, perPage: 0, query: '' };

  AuthzRolesDomain.loadRolesPaginated(getUnlimited).then(setPaginatedRoles);
};

const RolesSelector = ({ assignedRolesIds, onSubmit, identifier }: Props) => {
  const [paginatedRoles, setPaginatedRoles] = useState<PaginatedRoles | undefined>();
  const [selectedRoleName, setSelectedRoleName] = useState();
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | undefined>();

  useEffect(() => _loadRoles(setPaginatedRoles), []);

  if (!paginatedRoles) {
    return <Spinner />;
  }

  const _onSubmit = () => _assignRole(selectedRoleName, paginatedRoles.list, onSubmit, setSelectedRoleName, setIsSubmitting, setError);
  const options = _options(paginatedRoles.list, assignedRolesIds, identifier);

  return (
    <div>
      <FormElements>
        <StyledSelect inputProps={{ 'aria-label': 'Search for roles' }}
                      onChange={setSelectedRoleName}
                      optionRenderer={_renderRoleOption}
                      options={options}
                      placeholder="Search for roles"
                      multi
                      value={selectedRoleName} />
        <SubmitButton bsStyle="success"
                      onClick={_onSubmit}
                      disabled={isSubmitting || !selectedRoleName}
                      title="Assign Role"
                      type="button">
          Assign Role
        </SubmitButton>
      </FormElements>
      <ErrorAlert runtimeError onClose={setError}>
        {error}
      </ErrorAlert>
    </div>
  );
};

RolesSelector.defaultProps = {
  identifier: (role) => role.id,
};

RolesSelector.propTypes = {
  identifier: PropTypes.func,
  onSubmit: PropTypes.func.isRequired,
};

export default RolesSelector;
