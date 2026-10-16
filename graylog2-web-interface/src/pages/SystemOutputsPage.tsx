/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import React from 'react';
import { Trans, useTranslation } from 'react-i18next';

import { DocumentTitle, PageHeader } from 'components/common';
import OutputsComponent from 'components/outputs/OutputsComponent';
import useProductName from 'brand-customization/useProductName';
import MarketplaceLink from 'components/support/MarketplaceLink';

const SystemOutputsPage = () => {
  const { t } = useTranslation('streams');
  const productName = useProductName();

  return (
    <DocumentTitle title={t('system_outputs.title')}>
      <span>
        <PageHeader title={t('system_outputs.header_title')}>
          <span>
            <Trans
              t={t}
              i18nKey="system_outputs.description"
              values={{ productName }}
              components={{ highlight: <strong /> }}
            />
            <br />
            <MarketplaceLink prefix={t('system_outputs.marketplace_prefix')} />
          </span>
        </PageHeader>

        <OutputsComponent />
      </span>
    </DocumentTitle>
  );
};

export default SystemOutputsPage;
