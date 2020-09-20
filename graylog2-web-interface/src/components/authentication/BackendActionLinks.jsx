// @flow strict
import * as React from 'react';
import { LinkContainer } from 'react-router-bootstrap';

import AuthenticationBackend from 'logic/authentication/AuthenticationBackend';
import { Icon } from 'components/common';
import Routes from 'routing/Routes';
import { ButtonToolbar, Button } from 'components/graylog';

type Props = {
  activeBackend: ?AuthenticationBackend,
  finishedLoading: boolean,
};

const BackendActionLinks = ({ activeBackend, finishedLoading }: Props) => (
  <ButtonToolbar>
    <LinkContainer to={Routes.SYSTEM.AUTHENTICATION.PROVIDERS.edit(activeBackend?.id)}>
      <Button disabled={!activeBackend || !finishedLoading} type="button" bsStyle="success">
        Edit Active Service
      </Button>
    </LinkContainer>
    <LinkContainer to={Routes.SYSTEM.AUTHENTICATION.PROVIDERS.CREATE}>
      <Button typetype="button" bsStyle="success">
        Create Service
      </Button>
    </LinkContainer>
  </ButtonToolbar>
);
export default BackendActionLinks;
