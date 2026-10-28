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
import type { PropsWithChildren } from 'react';
import * as React from 'react';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import styled, { css } from 'styled-components';

import { Alert, Button } from 'components/bootstrap';
import { Icon, ConfirmDialog } from 'components/common';
import useProductName from 'brand-customization/useProductName';

const StyledAlert = styled(Alert)`
  padding: ${({ theme }) => theme.spacings.lg};
  margin: ${({ theme }) => theme.spacings.md};
  margin-top: 0;
`;

const Banner = styled.div`
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: space-between;
`;

const LeftItems = styled.div`
  display: flex;
  flex-direction: row;
  align-items: baseline;
  gap: ${({ theme }) => theme.spacings.md};
`;

const BoldText = styled.h1`
  font-weight: bold;
  color: ${({ theme }) => theme.colors.variant.danger};
`;

const Col = styled.div<{ $width?: string; $align?: string; $justify?: string }>`
  display: flex;
  flex-direction: column;
  gap: ${({ theme }) => theme.spacings.md};
  width: ${({ $width }) => $width || 'auto'};
  align-items: ${({ $align }) => $align || 'flex-start'};
`;

const Row = styled.div<{ $justify?: string; $fullWidth?: boolean }>`
  display: flex;
  flex-direction: row;
  ${({ $justify }) =>
    $justify
      ? css`
          justify-content: ${$justify};
        `
      : css`
          gap: ${({ theme }) => theme.spacings.md};
        `}
  width: ${({ $fullWidth }) => ($fullWidth ? '100%' : 'auto')};
`;

const StyledIcon = styled(Icon)`
  color: ${({ theme }) => theme.colors.variant.danger};
`;

const LEFT_COLUMN_ITEM_LIST = [
  'anomaly_detection_ai',
  'cloud_option',
  'archiving',
  'audit_logs_cloud',
  'dynamic_lookup_tables',
  'advanced_alerting',
  'compliance_reporting',
  'correlation_aggregation',
  'threat_intel',
];

const RIGHT_COLUMN_ITEM_LIST = [
  'incident_workspaces',
  'prebuilt_parser_dashboards',
  'sigma_rules',
  'parameterized_dashboards',
  'input_output_integrations',
  'threat_management',
  'search_workflow',
  'soar_integrations',
];

const TeaserPageLayout = ({ children }: PropsWithChildren) => {
  const { t } = useTranslation('users');
  const productName = useProductName();
  const [showModal, setShowModal] = useState(true);

  return (
    <>
      <StyledAlert bsStyle="info" noIcon>
        <Banner>
          <LeftItems>
            <BoldText>{t('security_teaser.demo.title')}</BoldText>
            <span>{t('security_teaser.demo.banner_text', { productName })}</span>
          </LeftItems>
          <Button bsStyle="primary" role="link" target="_blank" href="https://graylog.org/explore-security/">
            {t('security_teaser.demo.button', { productName })} <Icon name="open_in_new" />
          </Button>
        </Banner>
      </StyledAlert>
      {children}

      {showModal && (
        <ConfirmDialog
          show
          title={t('security_teaser.demo.title')}
          onConfirm={() => setShowModal(false)}
          onCancel={() => setShowModal(false)}
          btnConfirmText={t('common:actions.close')}>
          <Col>
            <h2 className="text-danger">{t('security_teaser.demo.overview')}</h2>
            <p>{t('security_teaser.demo.description', { productName })}</p>
            <Row $justify="space-between" $fullWidth>
              <Col>
                {LEFT_COLUMN_ITEM_LIST.map((item) => (
                  <Row key={item}>
                    <StyledIcon name="check" />
                    <span>{t(`security_teaser.demo.items.${item}`, { productName })}</span>
                  </Row>
                ))}
              </Col>
              <Col>
                {RIGHT_COLUMN_ITEM_LIST.map((item) => (
                  <Row key={item}>
                    <StyledIcon name="check" />
                    <span>{t(`security_teaser.demo.items.${item}`, { productName })}</span>
                  </Row>
                ))}
              </Col>
            </Row>
          </Col>
        </ConfirmDialog>
      )}
    </>
  );
};

export default TeaserPageLayout;
