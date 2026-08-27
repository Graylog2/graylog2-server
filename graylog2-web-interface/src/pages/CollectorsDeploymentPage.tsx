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
import { Navigate } from 'react-router-dom';
import styled, { css } from 'styled-components';

import { Row, Col, Tabs, Badge } from 'components/bootstrap';
import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import PreviewBadge from 'components/common/PreviewBadge';
import { DeployTab, EnrollmentTokenList } from 'components/collectors/deployment';
import { CollectorsPageNavigation } from 'components/collectors/common';
import { useCollectorsConfig, useEnrollmentTokenCount } from 'components/collectors/hooks';
import useSendCollectorsTelemetry from 'components/collectors/hooks/useSendCollectorsTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import { COLOR_SCHEME_LIGHT } from 'theme/constants';
import Routes from 'routing/Routes';

// Mantine's default --tab-border-color (gray-3 / dark-4) is too high-contrast against the content
// background. The palette vars don't flip with the color scheme, so pick per theme mode. Mantine
// defines the var via `[data-mantine-color-scheme] .class` (0-2-0), so the override needs higher
// specificity than a single class.
const SoftBorderTabs = styled(Tabs)(
  ({ theme }) => css`
    &&& {
      --tab-border-color: var(
        ${theme.mode === COLOR_SCHEME_LIGHT ? '--mantine-color-gray-1' : '--mantine-color-dark-5'}
      );
    }
  `,
);

const CollectorsDeploymentPage = () => {
  const { data: config, isLoading } = useCollectorsConfig();
  const tokenCount = useEnrollmentTokenCount();
  const sendTelemetry = useSendCollectorsTelemetry();

  const handleTabChange = (tab: string | null) => {
    if (!tab) return;

    sendTelemetry(TELEMETRY_EVENT_TYPE.COLLECTORS.DEPLOYMENT.TAB_SELECTED, {
      app_action_value: `tab-${tab}`,
      tab,
    });
  };

  if (isLoading) {
    return <Spinner />;
  }

  if (!config?.signing_cert_id) {
    return <Navigate to={Routes.SYSTEM.COLLECTORS.SETTINGS} />;
  }

  return (
    <DocumentTitle title="Deploy Collectors">
      <CollectorsPageNavigation />
      <PageHeader
        title={
          <>
            Deploy Collectors <PreviewBadge />
          </>
        }>
        <span>
          Run the command on any number of hosts &mdash; they enroll into the fleet you pick and appear as they check
          in.
        </span>
      </PageHeader>
      <Row className="content">
        <Col md={12}>
          <SoftBorderTabs defaultValue="deploy" onChange={handleTabChange}>
            <Tabs.List>
              <Tabs.Tab value="deploy">Deploy</Tabs.Tab>
              <Tabs.Tab value="tokens">
                Enrollment tokens
                {tokenCount !== undefined && (
                  <>
                    {' '}
                    <Badge>{tokenCount}</Badge>
                  </>
                )}
              </Tabs.Tab>
            </Tabs.List>
            <Tabs.Panel value="deploy">
              <DeployTab />
            </Tabs.Panel>
            <Tabs.Panel value="tokens">
              <p className="description">
                Tokens authorize new Collectors to enroll into a fleet. Deleting a token does not affect
                already-enrolled Collectors.
              </p>
              <EnrollmentTokenList />
            </Tabs.Panel>
          </SoftBorderTabs>
        </Col>
      </Row>
    </DocumentTitle>
  );
};

export default CollectorsDeploymentPage;
