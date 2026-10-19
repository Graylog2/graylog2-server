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
import { useTranslation } from 'react-i18next';

import useProductName from 'brand-customization/useProductName';
import { Row, Col } from 'components/bootstrap';
import { DocumentTitle, PageHeader } from 'components/common';
import DocsHelper from 'util/DocsHelper';
import DataNodeConfiguration from 'components/datanode/DataNodeConfiguration/DataNodeConfiguration';
import ClusterConfigurationPageNavigation from 'components/cluster-configuration/ClusterConfigurationPageNavigation';

const ClusterCertificateManagementPage = () => {
  const { t } = useTranslation('datanode');
  const productName = useProductName();

  return (
    <DocumentTitle title={t('pages.certificate_management_title')}>
      <ClusterConfigurationPageNavigation />
      <PageHeader
        title={t('pages.certificate_management_title')}
        documentationLink={{
          title: t('pages.documentation_title'),
          path: DocsHelper.PAGES.GRAYLOG_DATA_NODE,
        }}>
        <span>{t('pages.description', { productName })}</span>
      </PageHeader>
      <Row className="content">
        <Col md={12}>
          <DataNodeConfiguration />
        </Col>
      </Row>
    </DocumentTitle>
  );
};

export default ClusterCertificateManagementPage;
