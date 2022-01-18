import React, { useCallback } from 'react';
import type * as Immutable from 'immutable';

import { DataTable, EmptyResult } from 'components/common';
import type Role from 'logic/roles/Role';
import type { PaginatedRoles } from 'actions/roles/AuthzRolesActions';

import RolesOverviewItem from './RolesOverviewItem';

const TABLE_HEADERS = ['Name', 'Description', 'Users', 'Actions'];

type Props = {
  headerCellFormatter: (header: string) => React.ReactElement,
  paginatedRoles: PaginatedRoles,
  roles: Immutable.List<Role>,
  searchFilter: React.ReactNode,
};

const RolesTable = ({ headerCellFormatter, paginatedRoles, roles, searchFilter }: Props) => {
  const _rolesOverviewItem = useCallback((role) => {
    const { id } = role;

    return <RolesOverviewItem role={role} users={paginatedRoles?.context?.users[id]} />;
  }, [paginatedRoles?.context?.users]);

  return (
    <DataTable id="roles-overview"
               className="table-hover"
               rowClassName="no-bm"
               headers={TABLE_HEADERS}
               headerCellFormatter={headerCellFormatter}
               sortByKey="name"
               rows={roles.toJS()}
               noDataText={<EmptyResult>No roles have been found.</EmptyResult>}
               customFilter={searchFilter}
               dataRowFormatter={_rolesOverviewItem}
               filterKeys={[]}
               filterLabel="Filter Roles" />
  );
};

export default RolesTable;
