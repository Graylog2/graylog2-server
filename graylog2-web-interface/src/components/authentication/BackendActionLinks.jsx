// @flow strict
import * as React from 'react';

import { LinkContainer } from 'components/graylog/router';
import AuthenticationBackend from 'logic/authentication/AuthenticationBackend';
import Routes from 'routing/Routes';
import { ButtonToolbar, Button } from 'components/graylog';

type Props = {
  activeBackend: ?AuthenticationBackend,
  finishedLoading: boolean,
};

const BackendActionLinks = ({ activeBackend, finishedLoading }: Props) => (
  <ButtonToolbar>
    <LinkContainer to={Routes.SYSTEM.AUTHENTICATION.BACKENDS.edit(activeBackend?.id)}>
      <Button bsStyle="success"
              disabled={!activeBackend || !finishedLoading}
              type="button">
        Edit Active Service
      </Button>
    </LinkContainer>
    <LinkContainer to={Routes.SYSTEM.AUTHENTICATION.BACKENDS.CREATE}>
      <Button bsStyle="success" type="button">
        Create Service
      </Button>
    </LinkContainer>
  </ButtonToolbar>
);
export default BackendActionLinks;
