// @flow strict
import * as React from 'react';
import { useEffect, useState } from 'react';
import { withRouter } from 'react-router';
import { LinkContainer } from 'react-router-bootstrap';

import AuthzRolesDomain from 'domainActions/roles/AuthzRolesDomain';
import RoleDetails from 'components/roles/RoleDetails';
import RoleActionLinks from 'components/roles/navigation/RoleActionLinks';
import DocsHelper from 'util/DocsHelper';
import { PageHeader, DocumentTitle } from 'components/common';
import { Button } from 'components/graylog';
import DocumentationLink from 'components/support/DocumentationLink';
import Routes from 'routing/Routes';

type Props = {
  params: {
    roleId: string,
  },
};

const PageTitle = ({ fullName }: {fullName: ?string}) => (
  <>
    Role Details {fullName && (
      <>
        - <i>{fullName}</i>
      </>
  )}
  </>
);

const RoleDetailsPage = ({ params }: Props) => {
  const [loadedRole, setLoadedRole] = useState();
  const roleId = params?.roleId;

  useEffect(() => {
    AuthzRolesDomain.load(roleId).then(setLoadedRole);
  }, [roleId]);

  return (
    <DocumentTitle title={`Role Details ${loadedRole?.name ?? ''}`}>
      <PageHeader title={<PageTitle fullName={loadedRole?.name} />}
                  subactions={<RoleActionLinks roleId={roleId} />}>
        <span>
          Overview of details like name, description and assigned users.
        </span>
        <span>
          Learn more in the{' '}
          <DocumentationLink page={DocsHelper.PAGES.USERS_ROLES}
                             text="documentation" />
        </span>
        <LinkContainer to={Routes.SYSTEM.AUTHZROLES.OVERVIEW}>
          <Button bsStyle="info">Roles Overview</Button>
        </LinkContainer>
      </PageHeader>
      <RoleDetails role={roleId === loadedRole?.id ? loadedRole : undefined} />
    </DocumentTitle>
  );
};

export default withRouter(RoleDetailsPage);
