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
  readOnly: boolean,
  username: $PropertyType<UserOverview, 'username'>,
};

const ActionsWrapper = styled.div`
  display: flex;
  justify-content: flex-end;
`;

const EditTokensAction = ({
  username,
  wrapperComponent: WrapperComponent,
}: {
  username: $PropertyType<Props, 'username'>,
  wrapperComponent: Button | MenuItem,
}) => (
  <LinkContainer to={Routes.SYSTEM.USERS.TOKENS.edit(encodeURIComponent(username))}>
    <WrapperComponent id={`edit-tokens-${username}`}
                      bsStyle="info"
                      bsSize="xs"
                      title={`Edit tokens of user ${username}`}>
      Edit tokens
    </WrapperComponent>
  </LinkContainer>
);

const ReadOnlyActions = ({ username }: { username: $PropertyType<Props, 'username'> }) => {
  const tooltip = <Tooltip id="system-user">System users can only be modified in the Graylog configuration file.</Tooltip>;

  return (
    <>
      <OverlayTrigger placement="left" overlay={tooltip}>
        <Button bsSize="xs" bsStyle="info" disabled>System user</Button>
      </OverlayTrigger>
      &nbsp;
      <EditTokensAction username={username} wrapperComponent={Button} />
    </>
  );
};

const EditActions = ({ username }: { username: $PropertyType<Props, 'username'> }) => {
  const _deleteUser = () => {
    // eslint-disable-next-line no-alert
    if (window.confirm(`Do you really want to delete user ${username}?`)) {
      UsersDomain.delete(username);
    }
  };

  return (
    <>
      <IfPermitted permissions={[`users:edit:${username}`]}>
        <LinkContainer to={Routes.SYSTEM.USERS.edit(encodeURIComponent(username))}>
          <Button id={`edit-user-${username}`} bsStyle="info" bsSize="xs" title={`Edit user ${username}`}>
            Edit
          </Button>
        </LinkContainer>
      </IfPermitted>
      &nbsp;
      <DropdownButton bsSize="xs" title="More actions" pullRight id={`delete-user-${username}`}>
        <EditTokensAction username={username} wrapperComponent={MenuItem} />
        <IfPermitted permissions={[`users:edit:${username}`]}>
          <MenuItem id={`delete-user-${username}`}
                    bsStyle="primary"
                    bsSize="xs"
                    title={`Delete user ${username}`}
                    onClick={_deleteUser}>
            Delete
          </MenuItem>
        </IfPermitted>
      </DropdownButton>
    </>
  );
};

const ActionsCell = ({ username, readOnly }: Props) => {
  return (
    <td>
      <ActionsWrapper>
        {readOnly ? (
          <ReadOnlyActions username={username} />
        ) : (
          <EditActions username={username} />
        )}
      </ActionsWrapper>
    </td>
  );
};

export default ActionsCell;
