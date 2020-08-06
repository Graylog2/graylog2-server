// @flow strict
import * as React from 'react';
import { LinkContainer } from 'react-router-bootstrap';

import Role from 'logic/roles/Role';
import Routes from 'routing/Routes';
import { ButtonToolbar, Button } from 'components/graylog';

type Props = {
  roleId: $PropertyType<Role, 'id'>,
};

const RoleActionLinks = ({ roleId }: Props) => (
  <ButtonToolbar>
    <LinkContainer to={Routes.SYSTEM.ROLES.show(roleId)}>
      <Button bsStyle="success">
        View Details
      </Button>
    </LinkContainer>
    <LinkContainer to={Routes.SYSTEM.ROLES.edit(roleId)}>
      <Button bsStyle="success">
        Edit Role
      </Button>
    </LinkContainer>
  </ButtonToolbar>
);

export default RoleActionLinks;
