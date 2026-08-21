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
import { useState } from 'react';
import styled from 'styled-components';

import './types';

import useHasAccessToAnyStream from 'hooks/useHasAccessToAnyStream';
import usePluggableLicenseCheck from 'hooks/usePluggableLicenseCheck';
import usePluginEntities from 'hooks/usePluginEntities';
import { Alert, Row, Col, SegmentedControl } from 'components/bootstrap';
import Store from 'logic/local-storage/Store';
import { widgetActionsMenuClass } from 'views/components/widgets/Constants';
import SectionHeader from 'components/welcome/SectionHeader';

import MetricsSearchPage from './MetricsSearchPage';

const NO_STREAM_ACCESS_DISMISSED_KEY = 'welcome-metrics-no-stream-access-dismissed';

const Container = styled.div`
  margin-bottom: 6.4px;

  .${widgetActionsMenuClass} {
    opacity: 1;
  }
`;

const StyledAlert = styled(Alert)`
  margin: 0;
`;

const GENERAL_TAB_VALUE = 'general';

const WelcomeMetrics = () => {
  const hasAccessToAnyStream = useHasAccessToAnyStream();
  const [noStreamAccessDismissed, setNoStreamAccessDismissed] = useState(!!Store.get(NO_STREAM_ACCESS_DISMISSED_KEY));
  const [selectedTabValue, setSelectedTabValue] = useState<string>();

  const {
    data: { valid: isValidSecurityLicense },
  } = usePluggableLicenseCheck('/license/security');
  const pluginContext = { isValidSecurityLicense };

  const welcomePageMetricsPlugins = usePluginEntities('welcomePageMetrics');
  const activeExtraTabs = welcomePageMetricsPlugins.filter((plugin) =>
    typeof plugin.isEnabled === 'function' ? plugin.isEnabled(pluginContext) : true,
  );

  const generalPageMetricsPlugins = usePluginEntities('welcomePageMetrics.general');
  const activeGeneralPlugins = generalPageMetricsPlugins.filter((plugin) =>
    typeof plugin.isEnabled === 'function' ? plugin.isEnabled(pluginContext) : true,
  );

  const onDismissNoStreamAccess = () => {
    Store.set(NO_STREAM_ACCESS_DISMISSED_KEY, true);
    setNoStreamAccessDismissed(true);
  };

  if (!hasAccessToAnyStream) {
    if (noStreamAccessDismissed) {
      return null;
    }

    return (
      <Row className="content">
        <Col xs={12}>
          <StyledAlert onDismiss={onDismissNoStreamAccess}>
            Once you have access to a stream, your message metrics will show up here.
          </StyledAlert>
        </Col>
      </Row>
    );
  }

  if (activeExtraTabs.length === 0) {
    return (
      <>
        <SectionHeader>
          <h2>Overview</h2>
        </SectionHeader>
        <Container>
          <MetricsSearchPage />
        </Container>
      </>
    );
  }

  const GeneralTabComponent = activeGeneralPlugins[0]?.component ?? MetricsSearchPage;
  const tabs = [
    { label: 'General', value: GENERAL_TAB_VALUE, component: GeneralTabComponent },
    ...activeExtraTabs.map(({ label, component }) => ({ label, value: label, component })),
  ];
  const activeTabValue = selectedTabValue ?? activeExtraTabs[0].label;
  const ActiveTabComponent = tabs.find(({ value }) => value === activeTabValue)?.component ?? GeneralTabComponent;

  return (
    <>
      <SectionHeader>
        <h2>Overview</h2>
        <SegmentedControl
          data={tabs.map(({ label, value }) => ({ label, value }))}
          value={activeTabValue}
          onChange={setSelectedTabValue}
        />
      </SectionHeader>
      <Container>
        <ActiveTabComponent />
      </Container>
    </>
  );
};

export default WelcomeMetrics;
