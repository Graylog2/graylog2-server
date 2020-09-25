// @flow strict
/* eslint-disable no-alert */
import * as React from 'react';
import styled from 'styled-components';
import { LinkContainer } from 'react-router-bootstrap';
import { Link } from 'react-router';

import StringUtils from 'util/StringUtils';
import Routes from 'routing/Routes';
import AuthenticationDomain from 'domainActions/authentication/AuthenticationDomain';
import AuthenticationBackend from 'logic/authentication/AuthenticationBackend';
import { Button, ButtonToolbar } from 'components/graylog';

type Props = {
  authenticationBackend: AuthenticationBackend,
  isActive: boolean,
};

const StyledButtonToolbar = styled(ButtonToolbar)`
  display: flex;
  justify-content: flex-end;
`;

const EditButton = ({ isActive, authenticationBackend }: Props) => {
  const link = isActive
    ? Routes.SYSTEM.AUTHENTICATION.BACKENDS.ACTIVE : Routes.SYSTEM.AUTHENTICATION.BACKENDS.edit(authenticationBackend.id);

  return (
    <LinkContainer to={link}>
      <Button bsStyle="info" bsSize="xs" type="button">
        Edit
      </Button>
    </LinkContainer>
  );
};

const confirmMessage = (authBackendTitle: string, actionName: string) => {
  return `Do you really want to ${actionName} the authentication service "${StringUtils.truncateWithEllipses(authBackendTitle, 30)}"`;
};

const ActionsCell = ({ isActive, authenticationBackend }: Props) => {
  const { title, id } = authenticationBackend;
  const _setActiveBackend = (backendId) => AuthenticationDomain.setActiveBackend(backendId, title);

  const _deactiveBackend = () => {
    if (window.confirm(confirmMessage(title, 'deactivate'))) {
      _setActiveBackend(null);
    }
  };

  const _activateBackend = () => {
    if (window.confirm(confirmMessage(title, 'activate'))) {
      _setActiveBackend(id);
    }
  };

  const _deleteBackend = () => {
    if (window.confirm(confirmMessage(title, 'delete'))) {
      AuthenticationDomain.delete(id, title);
    }
  };

  return (
    <td className="limited">
      <StyledButtonToolbar>
        {isActive ? (
          <>
            <Button onClick={_deactiveBackend} bsStyle="warning" bsSize="xs" type="button">
              Deactivate
            </Button>
            <EditButton isActive={isActive} authenticationBackend={authenticationBackend} />
          </>
        ) : (
          <>
            <Button onClick={_activateBackend} bsStyle="warning" bsSize="xs" type="button">
              Activate
            </Button>
            <EditButton isActive={isActive} authenticationBackend={authenticationBackend} />
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
      <td className="limited">
        <Link to={Routes.SYSTEM.AUTHENTICATION.BACKENDS.show(id)}>
          {title}
        </Link>
      </td>
      <td className="limited">{description}</td>
      <ActionsCell authenticationBackend={authenticationBackend} isActive={isActive} />
    </tr>
  );
};

export default BackendsOverviewItem;
