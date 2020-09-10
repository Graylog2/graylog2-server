// @flow strict
import * as React from 'react';
import styled from 'styled-components';

import { Button, ButtonToolbar } from 'components/graylog';
import AuthenticationService from 'logic/authentication/AuthenticationService';

type Props = {
  authService: AuthenticationService,
  isActive: boolean,
};

const StyledButtonToolbar = styled(ButtonToolbar)`
  display: flex;
  justify-content: flex-end;
`;

const ActionsCell = ({ isActive }: { isActive: boolean }) => {
  return (
    <td className="limited">
      <StyledButtonToolbar>
        {isActive ? (
          <>
            <Button onClick={() => {}} bsStyle="info" bsSize="xs" type="button">
              Deactivate
            </Button>
            <Button onClick={() => {}} bsStyle="danger" bsSize="xs" type="button">
              Delete
            </Button>
          </>
        ) : (
          <Button onClick={() => {}} bsStyle="info" bsSize="xs" type="button">
            Activate
          </Button>
        )}
      </StyledButtonToolbar>
    </td>
  );
};

const AuthenticationServicesOverviewItem = ({ authService: { title, description, id }, isActive }: Props) => {
  return (
    <tr key={id} className={isActive ? 'active' : ''}>
      <td className="limited">{title}</td>
      <td className="limited">{description}</td>
      <ActionsCell isActive={isActive} />
    </tr>
  );
};

export default AuthenticationServicesOverviewItem;
