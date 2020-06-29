// @flow strict
import * as React from 'react';
import styled, { type StyledComponent } from 'styled-components';
import { Formik, Form } from 'formik';

import { type ThemeInterface } from 'theme';
import type { AvailableRoles } from 'logic/permissions/EntityShareState';
import type { Role, GRN } from 'logic/permissions/types';
import Grantee from 'logic/permissions/Grantee';
import { EntityShareActions } from 'stores/permissions/EntityShareStore';

import GranteeIcon from './GranteeIcon';
import RolesSelect from './RolesSelect';

const Container: StyledComponent<{}, ThemeInterface, HTMLDivElement> = styled.div`
  display: flex;
  align-items: flex-stretch;
  width: 100%;
`;

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
  entityGRN: GRN,
  grantee: Grantee,
  availableRoles: AvailableRoles,
  granteeRoleId: $PropertyType<Role, 'id'>,
};

const GranteesListItem = ({ entityGRN, grantee, availableRoles, granteeRoleId }: Props) => {
  const handleChange = (roleId) => {
    EntityShareActions.prepare(entityGRN, {
      selected_grantee_roles: {
        [grantee.id]: roleId,
      },
    });
  };

  return (
    <Formik initialValues={{ roleId: granteeRoleId }} onSubmit={() => {}}>
      <Form>
        <Container>
          <GranteeeInfo>
            <StyledGranteeIcon type={grantee.type} />
            {grantee.title}
          </GranteeeInfo>
          <StyledRolesSelect roles={availableRoles} onChange={handleChange} />
        </Container>
      </Form>
    </Formik>
  );
};

export default GranteesListItem;
