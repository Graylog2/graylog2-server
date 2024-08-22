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
import styled, { css } from 'styled-components';
import type { PropsWithChildren } from 'react';
import { useState } from 'react';

import { Alert, Button } from 'components/bootstrap';
import { Icon, ConfirmDialog } from 'components/common';
import { ContentArea, Container } from 'components/security/page-layout';

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

const Col = styled.div<{ $width?: string, $align?: string, $justify?: string }>`
  display: flex;
  flex-direction: column;
  gap: ${({ theme }) => theme.spacings.md};
  width: ${({ $width }) => $width || 'auto'};
  align-items: ${({ $align }) => $align || 'flex-start'};
`;

const Row = styled.div<{ $justify?: string, $fullWidth?: boolean }>`
  display: flex;
  flex-direction: row;
  ${({ $justify }) => (
    $justify ? css`justify-content: ${$justify};` : css`gap: ${({ theme }) => theme.spacings.md};`
  )}
  width: ${({ $fullWidth }) => ($fullWidth ? '100%' : 'auto')};
`;

const StyledIcon = styled(Icon)`
  color: ${({ theme }) => theme.colors.brand.primary};
`;

const LEFT_COLUMN_ITEM_LIST = [
  'Anomaly Detection AI',
  'Cloud Option',
  'Achiving',
  'Audit Logs for Graylog Cloud',
  'Dynamic Lookup Tables',
  'Advanced Alerting With Scripting',
  'Compliance Reporting',
  'Correlation & Aggregation Events',
  'Threat Intel Integrations',
];

const RIGHT_COLUMN_ITEM_LIST = [
  'Incident Investigation Workspaces',
  'Pre-build Security Parser & Dashboards',
  'Sigma Rules',
  'Parameterized Dashboarding',
  'Input & Output Integrations',
  'Threat Management',
  'Search Workflow, Temlplates & Filters',
  'Integrated Search & Alerting',
  'SOAR Integrations',
];

const TeaserPageLayout = ({ children }: PropsWithChildren) => {
  const [showModal, setShowModal] = useState(true);

  return (
    <>
      <Container>
        <ContentArea>
          <StyledAlert bsStyle="info" noIcon>
            <Banner>
              <LeftItems>
                <BoldText>Security Demo</BoldText>
                <span>For more information and booking a full demo of the product visit Graylog website.</span>
              </LeftItems>
              <Button bsStyle="primary" role="link" target="_blank" href="https://graylog.org/explore-security/">
                Graylog Security <Icon name="open_in_new" />
              </Button>
            </Banner>
          </StyledAlert>
          {children}
        </ContentArea>
      </Container>
      {showModal && (
        <ConfirmDialog show
                       title="Security Demo"
                       onConfirm={() => setShowModal(false)}
                       btnConfirmText="Close">
          <Col>
            <h2 className="text-danger">OVERVIEW</h2>
            <p>
              Graylog Security is designed to revolutionize cybersecurity for IT teams, offering the combined capabilities of SIEM,
              Security Analytics, Incident Investigation, and Anomaly Detection. By using our platform, you can work more efficiently,
              tackling critical tasks quicker, and mitigating risk caused by malicious actors and credential-based attacks.
            </p>
            <Row $justify="space-between" $fullWidth>
              <Col>
                {LEFT_COLUMN_ITEM_LIST.map((item) => (
                  <Row key={item}>
                    <StyledIcon name="check" />
                    <span>{item}</span>
                  </Row>
                ))}
              </Col>
              <Col>
                {RIGHT_COLUMN_ITEM_LIST.map((item) => (
                  <Row key={item}>
                    <StyledIcon name="check" />
                    <span>{item}</span>
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
