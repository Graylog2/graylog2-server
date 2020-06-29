// @flow strict
import * as React from 'react';

import type { GRN } from 'logic/permissions/types';
import type { AvailableGrantees, AvailableRoles, SelectedGranteeRoles } from 'logic/permissions/EntityShareState';

import GranteesListItem from './GranteesListItem';

const _userLookup = (userId: GRN, grantees: AvailableGrantees) => grantees.find((grantee) => grantee.id === userId);

type Props = {
  availableRoles: AvailableRoles,
  availableGrantees: AvailableGrantees,
  entityGRN: GRN,
  selectedGranteeRoles: SelectedGranteeRoles,
  className?: string,
};

const GranteesList = ({ entityGRN, availableRoles, availableGrantees, selectedGranteeRoles, className }: Props) => {
  return (
    <div className={className}>
      {selectedGranteeRoles.entrySeq().map(([granteeId, roleId]) => {
        const grantee = _userLookup(granteeId, availableGrantees);

        return grantee && (
          <GranteesListItem availableRoles={availableRoles}
                            grantee={grantee}
                            entityGRN={entityGRN}
                            key={grantee.id}
                            granteeRoleId={roleId} />
        );
      })}
    </div>
  );
};

GranteesList.defaultProps = {
  className: undefined,
};

export default GranteesList;
