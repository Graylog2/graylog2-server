// @flow strict
import * as React from 'react';
import styled, { type StyledComponent } from 'styled-components';
import { Formik, Form } from 'formik';

import { type ThemeInterface } from 'theme';
import EntityShareState, { type AvailableRoles } from 'logic/permissions/EntityShareState';
import Grantee from 'logic/permissions/Grantee';
import Role from 'logic/permissions/Role';
import SelectedGrantee from 'logic/permissions/SelectedGrantee';
import { IconButton } from 'components/common';

import GranteeIcon from './GranteeIcon';
import RolesSelect from './RolesSelect';

const Container: StyledComponent<{}, ThemeInterface, HTMLDivElement> = styled.div`
  display: flex;
  align-items: center;
  width: 100%;
  margin-bottom: 5px;
  padding: 5px;
`;

const GranteeeInfo = styled.div`
  display: flex;
  align-items: center;
  flex: 1;
`;

const StyledRolesSelect = styled(RolesSelect)`
  flex: 0.5;
`;

const StyledGranteeIcon = styled(GranteeIcon)`
  margin-right: 5px;
`;

const DeleteIcon = styled(IconButton)`
  margin-left: 10px;
`;

type Props = {
  grantee: SelectedGrantee,
  availableRoles: AvailableRoles,
  onRoleChange: ({
    granteeId: $PropertyType<Grantee, 'id'>,
    roleId: $PropertyType<Role, 'id'>,
  }) => Promise<EntityShareState>,
  onDelete: ($PropertyType<Grantee, 'id'>) => Promise<EntityShareState>,
};

const GranteesListItem = ({ onDelete, onRoleChange, grantee: { id, roleId, type, title }, availableRoles }: Props) => {
  return (
    <Formik initialValues={{ roleId }} onSubmit={() => {}}>
      <Form>
        <Container>
          <GranteeeInfo>
            <StyledGranteeIcon type={type} />
            {title}
          </GranteeeInfo>
          <StyledRolesSelect roles={availableRoles} onChange={(newRoleId) => onRoleChange({ granteeId: id, roleId: newRoleId })} />
          <DeleteIcon name="trash" onClick={() => onDelete(id)} title={`Delete sharing for ${title}`} />
        </Container>
      </Form>
    </Formik>
  );
};

export default GranteesListItem;
