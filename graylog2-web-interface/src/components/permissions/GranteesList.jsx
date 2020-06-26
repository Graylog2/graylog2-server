// @flow strict
import * as React from 'react';
import styled from 'styled-components';

import type { GRN } from 'logic/permissions/types';
import type { AvailableGrantees, ActiveShares, AvailableRoles, SelectedGranteeRoles } from 'logic/permissions/EntityShareState';

import GranteesListItem from './GranteesListItem';

const _userLookup = (userId: GRN, grantees: AvailableGrantees) => grantees.find((grantee) => grantee.id === userId);

type Props = {
  activeShares: ActiveShares,
  availableRoles: AvailableRoles,
  availableGrantees: AvailableGrantees,
  selectedGranteeRoles: SelectedGranteeRoles,
  className?: string,
};

const GranteesList = ({ activeShares, availableRoles, availableGrantees, selectedGranteeRoles, className }: Props) => {
  return (
    <div className={className}>
      {activeShares.map(({ grantee: granteeId, role: roleId }) => {
        const grantee = _userLookup(granteeId, availableGrantees);

        return grantee && <GranteesListItem availableRoles={availableRoles} grantee={grantee} granteeRoleId={roleId} />;
      })}
      {selectedGranteeRoles.entrySeq().map(([granteeId, roleId]) => {
        const grantee = _userLookup(granteeId, availableGrantees);

        return grantee && <GranteesListItem availableRoles={availableRoles} grantee={grantee} granteeRoleId={roleId} />;
      })}
    </div>
  );
};

GranteesList.defaultProps = {
  className: undefined,
};

export default GranteesList;
