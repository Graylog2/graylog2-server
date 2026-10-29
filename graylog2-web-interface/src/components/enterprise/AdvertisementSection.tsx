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
import styled from 'styled-components';
import { PluginStore } from 'graylog-web-plugin/plugin';

import { Col, Row } from 'components/bootstrap';
import useProductName from 'brand-customization/useProductName';
import ProductLink from 'components/enterprise/ProductLink';
import { useStore } from 'stores/connect';
import { NodesStore } from 'stores/nodes/NodesStore';

const GraylogEnterpriseHeader = styled.h2`
  margin-bottom: 10px;
`;

const AdvertisementSection = () => {
  const { t } = useTranslation('common');
  const nodes = useStore(NodesStore);
  const productName = useProductName();
  const licensePlugin = PluginStore.exports('license');
  const ProductLinkComponent = licensePlugin[0]?.EnterpriseProductLink || ProductLink;
  const { clusterId } = nodes;

  return (
    <Row className="content">
      <Col md={6}>
        <GraylogEnterpriseHeader>
          {t('enterprise_advertisement.enterprise_title', { productName })}
        </GraylogEnterpriseHeader>
        <p>{t('enterprise_advertisement.enterprise_description', { productName })}</p>
        <ProductLinkComponent href="https://go2.graylog.org/request-graylog-operations" clusterId={clusterId}>
          {t('enterprise_advertisement.request_now')}
        </ProductLinkComponent>
      </Col>
      <Col md={6}>
        <GraylogEnterpriseHeader>
          {t('enterprise_advertisement.security_title', { productName })}
        </GraylogEnterpriseHeader>
        <p>{t('enterprise_advertisement.security_description', { productName })}</p>
        <ProductLinkComponent
          href="https://go2.graylog.org/request-graylog-security"
          licenseSubject="/license/security"
          clusterId={clusterId}>
          {t('enterprise_advertisement.request_now')}
        </ProductLinkComponent>
      </Col>
    </Row>
  );
};

export default AdvertisementSection;
