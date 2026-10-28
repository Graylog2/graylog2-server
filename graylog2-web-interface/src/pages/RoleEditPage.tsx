import * as React from 'react';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';

import withParams from 'routing/withParams';
import { LinkContainer, PageHeader, DocumentTitle } from 'components/common';
import RoleEdit from 'components/roles/RoleEdit';
import Routes from 'routing/Routes';
import RoleActionLinks from 'components/roles/navigation/RoleActionLinks';
import { Button } from 'components/bootstrap';
import { loadRole } from 'hooks/useAuthzRoles';
import DocsHelper from 'util/DocsHelper';
import type Role from 'logic/roles/Role';

type Props = {
  params: {
    roleId: string;
  };
};

const PageTitle = ({ name }: { name: string | undefined | null }) => {
  const { t } = useTranslation('users');

  return (
    <>
      {t('role_edit.title_prefix')}{' '}
      {name && (
        <>
          - <i>{name}</i>
        </>
      )}
    </>
  );
};

const RoleEditPage = ({ params }: Props) => {
  const { t } = useTranslation('users');
  const [loadedRole, setLoadedRole] = useState<Role | undefined>();
  const roleId = params?.roleId;

  useEffect(() => {
    loadRole(roleId).then(setLoadedRole);
  }, [roleId]);

  return (
    <DocumentTitle title={t('role_edit.title', { name: loadedRole?.name ?? '' })}>
      <PageHeader
        title={<PageTitle name={loadedRole?.name} />}
        actions={<RoleActionLinks roleId={roleId} />}
        topActions={
          <LinkContainer to={Routes.SYSTEM.AUTHZROLES.OVERVIEW}>
            <Button bsStyle="info">{t('role_edit.button_text')}</Button>
          </LinkContainer>
        }
        documentationLink={{
          title: t('role_edit.documentation_link_title'),
          path: DocsHelper.PAGES.USERS_ROLES,
        }}>
        <span >{t('role_edit.description')}</span>
      </PageHeader>
      <RoleEdit role={roleId === loadedRole?.id ? loadedRole : undefined} />
    </DocumentTitle>
  );
};

export default withParams(RoleEditPage);