// @flow strict
import * as React from 'react';

import { LinkContainer } from 'components/graylog/router';
import Routes from 'routing/Routes';
import { Button } from 'components/graylog';

type Props = {
  authenticationBackendId: string,
  stepKey: string,
};

const EditLinkButton = ({ authenticationBackendId, stepKey }: Props) => (
  <LinkContainer to={Routes.SYSTEM.AUTHENTICATION.BACKENDS.edit(authenticationBackendId, stepKey)}>
    <Button bsStyle="success" bsSize="small">Edit</Button>
  </LinkContainer>
);

export default EditLinkButton;
