// @flow strict
import * as React from 'react';
import styled from 'styled-components';

import { LinkContainer } from 'components/graylog/router';
import UserOverview from 'logic/users/UserOverview';
import UsersDomain from 'domainActions/users/UsersDomain';
import Routes from 'routing/Routes';
import { Button, OverlayTrigger, Tooltip, DropdownButton, MenuItem } from 'components/graylog';
import { IfPermitted } from 'components/common';

type Props = {
  user: UserOverview,
};

const ActionsWrapper = styled.div`
  display: flex;
  justify-content: flex-end;
`;

const EditTokensAction = ({
  user: { fullName, id },
  wrapperComponent: WrapperComponent,
}: {
  user: UserOverview,
  wrapperComponent: Button | MenuItem,
}) => (
  <LinkContainer to={Routes.SYSTEM.USERS.TOKENS.edit(id)}>
    <WrapperComponent id={`edit-tokens-${id}`}
                      bsStyle="info"
                      bsSize="xs"
                      title={`Edit tokens of user ${fullName}`}>
      Edit tokens
    </WrapperComponent>
  </LinkContainer>
);

const ReadOnlyActions = ({ user }: { user: UserOverview }) => {
  const tooltip = <Tooltip id="system-user">System users can only be modified in the Graylog configuration file.</Tooltip>;

  return (
    <>
      <OverlayTrigger placement="left" overlay={tooltip}>
        <Button bsSize="xs" bsStyle="info" disabled>System user</Button>
      </OverlayTrigger>
      &nbsp;
      <EditTokensAction user={user} wrapperComponent={Button} />
    </>
  );
};

const EditActions = ({ user, user: { username, id, fullName } }: { user: UserOverview }) => {
  const _deleteUser = () => {
    // eslint-disable-next-line no-alert
    if (window.confirm(`Do you really want to delete user ${fullName}?`)) {
      UsersDomain.delete(username, fullName);
    }
  };

  return (
    <>
      <IfPermitted permissions={[`users:edit:${username}`]}>
        <LinkContainer to={Routes.SYSTEM.USERS.edit(id)}>
          <Button id={`edit-user-${id}`} bsStyle="info" bsSize="xs" title={`Edit user ${fullName}`}>
            Edit
          </Button>
        </LinkContainer>
      </IfPermitted>
      &nbsp;
      <DropdownButton bsSize="xs" title="More actions" pullRight id={`delete-user-${id}`}>
        <EditTokensAction user={user} wrapperComponent={MenuItem} />
        <IfPermitted permissions={[`users:edit:${username}`]}>
          <MenuItem id={`delete-user-${id}`}
                    bsStyle="primary"
                    bsSize="xs"
                    title={`Delete user ${fullName}`}
                    onClick={_deleteUser}>
            Delete
          </MenuItem>
        </IfPermitted>
      </DropdownButton>
    </>
  );
};

const ActionsCell = ({ user }: Props) => (
  <td>
    <ActionsWrapper>
      {user.readOnly ? (
        <ReadOnlyActions user={user} />
      ) : (
        <EditActions user={user} />
      )}
    </ActionsWrapper>
  </td>
);

export default ActionsCell;
