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
import type Role from 'logic/roles/Role';
import { Button } from 'components/bootstrap';
import { Select, Spinner, ErrorAlert } from 'components/common';

type Props = {
  assignedRolesIds: Immutable.Set<string>,
  identifier: (role: Role) => string,
  onSubmit: (role: Immutable.Set<Role>) => Promise<void>,
  submitOnSelect?: boolean,
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

const _assignRole = (selectedRoleNames, roles, onSubmit, setselectedRoleNames, setIsSubmitting, setError) => {
  if (!selectedRoleNames) {
    return;
  }

  const selectedRoleNameList = selectedRoleNames.split(',');

  const selectedRoles = Immutable.Set(compact(selectedRoleNameList.map((selection) => {
    return roles.find((r) => r.name === selection);
  })));

  if (selectedRoles.size <= 0) {
    setError(`Role assignment failed, because the roles ${selectedRoleNames ?? '(undefined)'} does not exist`);

    return;
  }

  setError();
  setIsSubmitting(true);

  onSubmit(selectedRoles).then(() => {
    setselectedRoleNames();
    setIsSubmitting(false);
  });
};

const _loadRoles = (setPaginatedRoles) => {
  const getUnlimited = { page: 1, perPage: 0, query: '' };

  AuthzRolesDomain.loadRolesPaginated(getUnlimited).then(setPaginatedRoles);
};

const RolesSelector = ({ assignedRolesIds, onSubmit, identifier, submitOnSelect }: Props) => {
  const [paginatedRoles, setPaginatedRoles] = useState<PaginatedRoles | undefined>();
  const [selectedRoleNames, setselectedRoleNames] = useState<string | undefined>();
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | undefined>();

  useEffect(() => _loadRoles(setPaginatedRoles), []);

  const onChange = (items) => {
    const newselectedRoleNameList = items;

    if (submitOnSelect) {
      _assignRole(newselectedRoleNameList, paginatedRoles.list, onSubmit, setselectedRoleNames, setIsSubmitting, setError);
    }

    setselectedRoleNames(items);
  };

  if (!paginatedRoles) {
    return <Spinner />;
  }

  const _onSubmit = () => _assignRole(selectedRoleNames, paginatedRoles.list, onSubmit, setselectedRoleNames, setIsSubmitting, setError);
  const options = _options(paginatedRoles.list, assignedRolesIds, identifier);

  return (
    <div>
      <FormElements>
        <StyledSelect inputProps={{ 'aria-label': 'Search for roles' }}
                      onChange={onChange}
                      optionRenderer={_renderRoleOption}
                      options={options}
                      placeholder="Search for roles"
                      multi
                      value={selectedRoleNames} />
        {!submitOnSelect && (
        <SubmitButton bsStyle="success"
                      onClick={_onSubmit}
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
  submitOnSelect: false,
};

RolesSelector.propTypes = {
  identifier: PropTypes.func,
  onSubmit: PropTypes.func.isRequired,
  submitOnSelect: PropTypes.bool,
};

export default RolesSelector;
