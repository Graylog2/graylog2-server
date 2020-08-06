// @flow strict
import * as React from 'react';
import { useEffect, useState } from 'react';
import { withRouter } from 'react-router';
import { LinkContainer } from 'react-router-bootstrap';

import RoleEdit from 'components/roles/RoleEdit';
import Routes from 'routing/Routes';
import RoleActionLinks from 'components/roles/navigation/RoleActionLinks';
import { Button } from 'components/graylog';
import { AuthzRolesActions } from 'stores/roles/AuthzRolesStore';
import DocsHelper from 'util/DocsHelper';
import { PageHeader, DocumentTitle } from 'components/common';
import DocumentationLink from 'components/support/DocumentationLink';

type Props = {
  params: {
    roleId: string,
  },
};

const PageTitle = ({ name }: {name: ?string}) => (
  <>
    Edit Role {name && (
      <>
        - <i>{name}</i>
      </>
  )}
  </>
);

const RoleEditPage = ({ params }: Props) => {
  const [loadedRole, setLoadedRole] = useState();
  const roleId = params?.roleId;

  useEffect(() => {
    AuthzRolesActions.load(roleId).then(setLoadedRole);
  }, []);

  return (
    <DocumentTitle title={`Edit Role ${loadedRole?.name ?? ''}`}>
      <PageHeader title={<PageTitle name={loadedRole?.name} />}>
        <span>
          You can assign the role to users.
        </span>
        <span>
          Learn more in the{' '}
          <DocumentationLink page={DocsHelper.PAGES.USERS_ROLES}
                             text="documentation" />
        </span>
        <LinkContainer to={Routes.SYSTEM.AUTHZROLES.OVERVIEW}>
          <Button bsStyle="info">Roles Overview</Button>
        </LinkContainer>
        {/* This needs to be defined for the page header, once we've merge the required PR */}
        <RoleActionLinks roleId={roleId} />
      </PageHeader>
      <RoleEdit role={roleId === loadedRole?.id ? loadedRole : undefined} />
    </DocumentTitle>
  );
};

export default withRouter(RoleEditPage);
