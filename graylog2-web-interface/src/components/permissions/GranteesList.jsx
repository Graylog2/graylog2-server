// @flow strict
import * as React from 'react';
import styled, { type StyledComponent } from 'styled-components';

import type { GRN } from 'logic/permissions/types';
import EntityShareState, { type ActiveShares, type AvailableRoles, type SelectedGrantees } from 'logic/permissions/EntityShareState';
import Grantee from 'logic/permissions/Grantee';
import Role from 'logic/permissions/Role';
import { type ThemeInterface } from 'theme';

import GranteesListItem from './GranteesListItem';

const Container: StyledComponent<{}, ThemeInterface, HTMLDivElement> = styled.div(({ theme }) => `
  >:nth-child(even) {
    background: ${theme.colors.table.backgroundAlt};
  };
  >:nth-child(odd) {
    background: ${theme.colors.table.background};
  };
`);

type Props = {
  activeShares: ActiveShares,
  availableRoles: AvailableRoles,
  className?: string,
  entityGRN: GRN,
  onDelete: (GRN) => Promise<EntityShareState>,
  onRoleChange: ({
    granteeId: $PropertyType<Grantee, 'id'>,
    roleId: $PropertyType<Role, 'id'>,
  }) => Promise<EntityShareState>,
  selectedGrantees: SelectedGrantees,
};

const GranteesList = ({ activeShares, onDelete, onRoleChange, entityGRN, availableRoles, selectedGrantees, className }: Props) => (
  <Container className={className}>
    {selectedGrantees.map((grantee) => {
      const currentGranteeState = grantee.currentState(activeShares);

      return (
        <GranteesListItem availableRoles={availableRoles}
                          currentGranteeState={currentGranteeState}
                          entityGRN={entityGRN}
                          grantee={grantee}
                          key={grantee.id}
                          onDelete={onDelete}
                          onRoleChange={onRoleChange} />
      );
    })}
  </Container>
);

GranteesList.defaultProps = {
  className: undefined,
};

export default GranteesList;
