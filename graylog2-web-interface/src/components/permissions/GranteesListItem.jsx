// @flow strict
import * as React from 'react';
import styled, { type StyledComponent } from 'styled-components';

import { type ThemeInterface } from 'theme';
import type { AvailableRoles } from 'logic/permissions/EntityShareState';
import Grantee from 'logic/permissions/Grantee';

import GranteeIcon from './GranteeIcon';
import RolesSelect from './RolesSelect';

const Container: StyledComponent<{}, ThemeInterface, HTMLDivElement> = styled.div(({ theme }) => `
  display: flex;
  align-items: flex-stretch;
  width: 100%;
`);

const GranteeeInfo = styled.div`
  flex: 1;
`;

const StyledRolesSelect = styled(RolesSelect)`
  flex: 0.5;
`;

const StyledGranteeIcon = styled(GranteeIcon)`
  margin-right: 5px;
`;

type Props = {
  grantee: Grantee,
  availableRoles: AvailableRoles,
};

const GranteesListItem = ({ grantee, availableRoles }: Props) => {
  return (
    <Container>
      <GranteeeInfo>
        <StyledGranteeIcon type={grantee.type} />
        {grantee.title}
      </GranteeeInfo>
      <StyledRolesSelect roles={availableRoles} />
    </Container>
  );
};

export default GranteesListItem;
