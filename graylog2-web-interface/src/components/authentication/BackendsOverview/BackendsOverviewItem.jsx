// @flow strict
import * as React from 'react';
import styled from 'styled-components';

import AuthenticationDomain from 'domainActions/authentication/AuthenticationDomain';
import { Button, ButtonToolbar } from 'components/graylog';
import AuthenticationBackend from 'logic/authentication/AuthenticationBackend';

type Props = {
  authenticationBackend: AuthenticationBackend,
  isActive: boolean,
};

const StyledButtonToolbar = styled(ButtonToolbar)`
  display: flex;
  justify-content: flex-end;
`;

const ActionsCell = ({ isActive, authenticationBackend }: Props) => {
  const { title, id } = authenticationBackend;
  const _setActiveBackend = (backendId: ?$PropertyType<AuthenticationBackend, 'id'>) => AuthenticationDomain.setActiveBackend(backendId, title);
  const _deleteBackend = AuthenticationDomain.delete(id, title);

  return (
    <td className="limited">
      <StyledButtonToolbar>
        {isActive ? (
          <Button onClick={() => _setActiveBackend(null)} bsStyle="info" bsSize="xs" type="button">
            Deactivate
          </Button>
        ) : (
          <>
            <Button onClick={() => _setActiveBackend(id)} bsStyle="info" bsSize="xs" type="button">
              Activate
            </Button>
            <Button onClick={_deleteBackend} bsStyle="danger" bsSize="xs" type="button">
              Delete
            </Button>
          </>
        )}
      </StyledButtonToolbar>
    </td>
  );
};

const BackendsOverviewItem = ({ authenticationBackend, isActive }: Props) => {
  const { title, description, id } = authenticationBackend;

  return (
    <tr key={id} className={isActive ? 'active' : ''}>
      <td className="limited">{title}</td>
      <td className="limited">{description}</td>
      <ActionsCell isActive={isActive} authenticationBackend={authenticationBackend} />
    </tr>
  );
};

export default BackendsOverviewItem;
