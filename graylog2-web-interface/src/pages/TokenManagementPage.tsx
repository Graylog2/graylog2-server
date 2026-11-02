import * as React from 'react';
import { useTranslation } from 'react-i18next';

import { PageHeader, DocumentTitle } from 'components/common';
import UsersPageNavigation from 'components/users/navigation/UsersPageNavigation';
import TokenManagement from 'components/users/UsersTokenManagement/TokenManagement';
import useProductName from 'brand-customization/useProductName';

const TokenManagementPage = () => {
  const { t } = useTranslation('users');
  const productName = useProductName();

  return (
    <DocumentTitle title={t('token_management.title')}>
      <UsersPageNavigation />
      <PageHeader title={t('token_management.title')}>
        <span>{t('token_management.description', { productName })}</span>
      </PageHeader>

      <TokenManagement />
    </DocumentTitle>
  );
};

export default TokenManagementPage;
