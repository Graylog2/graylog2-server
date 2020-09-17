// @flow strict
import * as React from 'react';
import { LinkContainer } from 'react-router-bootstrap';

import AuthenticationBackend from 'logic/authentication/AuthenticationBackend';
import { Icon, Spinner } from 'components/common';
import Routes from 'routing/Routes';
import { ButtonToolbar, MenuItem, SplitButton } from 'components/graylog';

type Props = {
  activeBackend: ?AuthenticationBackend,
  finishedLoading: boolean,
};

const BackendOverviewLinks = ({ activeBackend, finishedLoading }: Props) => (
  <ButtonToolbar>
    <LinkContainer to={Routes.SYSTEM.AUTHENTICATION.PROVIDERS.ACTIVE}>
      <SplitButton title="Active Service"
                   id="active-backend-dropdown"
                   pullRight
                   bsStyle="info">
        {finishedLoading ? (
          <MenuItem disabled={!activeBackend} href={Routes.SYSTEM.AUTHENTICATION.PROVIDERS.edit(activeBackend?.id)}>
            <><Icon name="edit" /> Edit</>
          </MenuItem>
        ) : (
          <Spinner />
        )}
      </SplitButton>
    </LinkContainer>
    <LinkContainer to={Routes.SYSTEM.AUTHENTICATION.PROVIDERS.OVERVIEW}>
      <SplitButton title="All Service"
                   id="all-services-dropdown"
                   pullRight
                   bsStyle="info">
        {finishedLoading ? (
          <MenuItem href={Routes.SYSTEM.AUTHENTICATION.PROVIDERS.CREATE}>
            <><Icon name="plus" /> Create</>
          </MenuItem>
        ) : (
          <Spinner />
        )}
      </SplitButton>
    </LinkContainer>
  </ButtonToolbar>
);
export default BackendOverviewLinks;
