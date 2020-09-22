// @flow strict
import * as React from 'react';
import { LinkContainer } from 'react-router-bootstrap';

import Routes from 'routing/Routes';
import { Button } from 'components/graylog';

type Props = {
  authenticationBackendId: string,
  stepKey: string,
};

const EditLinkButton = ({ authenticationBackendId, stepKey }: Props) => {
  const editLink = {
    pathname: Routes.SYSTEM.AUTHENTICATION.PROVIDERS.edit(authenticationBackendId),
    query: {
      initialStepKey: stepKey,
    },
  };

  return (
    <LinkContainer to={editLink}>
      <Button bsStyle="success" bsSize="small">Edit</Button>
    </LinkContainer>
  );
};

export default EditLinkButton;
