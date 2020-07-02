// @flow strict
import * as React from 'react';
import { useState } from 'react';
import styled, { type StyledComponent } from 'styled-components';
import { Formik, Form } from 'formik';

import { type ThemeInterface } from 'theme';
import EntityShareState, { type AvailableRoles } from 'logic/permissions/EntityShareState';
import Grantee from 'logic/permissions/Grantee';
import { Spinner, IconButton } from 'components/common';
import Role from 'logic/permissions/Role';
import SelectedGrantee, { type CurrentState as CurrentGranteeState } from 'logic/permissions/SelectedGrantee';

import GranteeIcon from './GranteeIcon';
import RolesSelect from './RolesSelect';

const currentStateColor = (theme: ThemeInterface, currentState: CurrentGranteeState) => {
  switch (currentState) {
    case 'new':
      return theme.colors.variant.lighter.success;
    case 'changed':
      return theme.colors.variant.lighter.warning;
    default:
      return 'transparent';
  }
};

const Container: StyledComponent<{ currentState: CurrentGranteeState }, ThemeInterface, HTMLDivElement> = styled.div(({ theme, currentState }) => `
  display: flex;
  align-items: center;
  width: 100%;
  margin-bottom: 5px;
  padding: 5px;
  border-left: 5px solid ${currentStateColor(theme, currentState)};
`);

const GranteeeInfo = styled.div`
  display: flex;
  align-items: center;
  flex: 1;
  overflow: hidden;
  margin-right: 10px;
`;

const Title = styled.div`
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
`;

const StyledRolesSelect = styled(RolesSelect)`
  flex: 0.5;
`;

const StyledGranteeIcon = styled(GranteeIcon)`
  margin-right: 5px;
`;

const Actions = styled.div`
  display: flex;
  align-items: center;
  justify-content: center;
  width: 25px;
  margin-left: 10px;
`;

type Props = {
  availableRoles: AvailableRoles,
  currentGranteeState: CurrentGranteeState,
  grantee: SelectedGrantee,
  onDelete: ($PropertyType<Grantee, 'id'>) => Promise<EntityShareState>,
  onRoleChange: ({
    granteeId: $PropertyType<Grantee, 'id'>,
    roleId: $PropertyType<Role, 'id'>,
  }) => Promise<EntityShareState>,
};

const GranteesListItem = ({ availableRoles, currentGranteeState, grantee: { id, roleId, type, title }, onDelete, onRoleChange }: Props) => {
  const [isDeleting, setIsDeleting] = useState(false);

  const handleDelete = () => {
    setIsDeleting(true);

    onDelete(id).then(() => setIsDeleting(false));
  };

  return (
    <Formik initialValues={{ roleId }} onSubmit={() => {}}>
      <Form>
        <Container currentState={currentGranteeState}>
          <GranteeeInfo>
            <StyledGranteeIcon type={type} />
            <Title>{title}</Title>
          </GranteeeInfo>
          <StyledRolesSelect onChange={(newRoleId) => onRoleChange({ granteeId: id, roleId: newRoleId })}
                             roles={availableRoles}
                             title={`Change the role for ${title}`} />
          <Actions>
            {isDeleting ? (
              <Spinner text="" />
            ) : (
              <IconButton name="trash" onClick={handleDelete} title={`Delete sharing for ${title}`} />
            )}
          </Actions>
        </Container>
      </Form>
    </Formik>
  );
};

export default GranteesListItem;
