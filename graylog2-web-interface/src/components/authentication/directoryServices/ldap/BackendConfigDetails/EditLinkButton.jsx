// @flow strict
import * as React from 'react';
import URI from 'urijs';

import { LinkContainer } from 'components/graylog/router';
import Routes from 'routing/Routes';
import { Button } from 'components/graylog';

type Props = {
  authenticationBackendId: string,
  stepKey: string,
};

const EditLinkButton = ({ authenticationBackendId, stepKey }: Props) => {
  const editLink = new URI(Routes.SYSTEM.AUTHENTICATION.BACKENDS.edit(authenticationBackendId))
    .search({ initialStepKey: stepKey })
    .toString();

  return (
    <LinkContainer to={editLink}>
      <Button bsStyle="success" bsSize="small">Edit</Button>
    </LinkContainer>
  );
};

export default EditLinkButton;
