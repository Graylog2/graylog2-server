// @flow strict
import * as React from 'react';
import styled from 'styled-components';

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

const ActionsCell = ({ isActive }: { isActive: boolean }) => (
  <td className="limited">
    <StyledButtonToolbar>
      {isActive ? (
        <Button onClick={() => {}} bsStyle="info" bsSize="xs" type="button">
          Deactivate
        </Button>
      ) : (
        <>
          <Button onClick={() => {}} bsStyle="info" bsSize="xs" type="button">
            Activate
          </Button>
          <Button onClick={() => {}} bsStyle="danger" bsSize="xs" type="button">
            Delete
          </Button>
        </>
      )}
    </StyledButtonToolbar>
  </td>
);

const BackendsOverviewItem = ({ authenticationBackend: { title, description, id }, isActive }: Props) => (
  <tr key={id} className={isActive ? 'active' : ''}>
    <td className="limited">{title}</td>
    <td className="limited">{description}</td>
    <ActionsCell isActive={isActive} />
  </tr>
);

export default BackendsOverviewItem;
