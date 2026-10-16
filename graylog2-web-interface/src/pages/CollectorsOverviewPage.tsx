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
import * as React from 'react';
import { useTranslation } from 'react-i18next';

import { Row, Col } from 'components/bootstrap';
import { DocumentTitle, PageHeader } from 'components/common';
import useProductName from 'brand-customization/useProductName';
import PreviewBadge from 'components/common/PreviewBadge';
import DocsHelper from 'util/DocsHelper';
import { CollectorsOverview } from 'components/collectors/overview';
import { CollectorsPageNavigation } from 'components/collectors/common';

const CollectorsOverviewPage = () => {
  const { t } = useTranslation('collectors_overview');
  const productName = useProductName();

  return (
    <DocumentTitle title={t('title')}>
      <CollectorsPageNavigation />
      <PageHeader
        title={
          <>
            {t('overview_title')} <PreviewBadge />
          </>
        }
        documentationLink={{
          title: t('documentation_title'),
          path: DocsHelper.PAGES.COLLECTORS,
        }}>
        <span>
          {t('description', { productName })}
        </span>
      </PageHeader>
      <Row className="content">
        <Col md={12}>
          <CollectorsOverview />
        </Col>
      </Row>
    </DocumentTitle>
  );
};

export default CollectorsOverviewPage;
