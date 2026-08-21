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
import { SegmentedControl } from 'components/bootstrap';
import { widgetActionsMenuClass } from 'views/components/widgets/Constants';
import SectionHeader from 'components/welcome/SectionHeader';
import NoStreamAccessAlert from 'components/welcome/NoStreamAccessAlert';

import MetricsSearchPage from './MetricsSearchPage';

const Container = styled.div`
  margin-bottom: 6.4px;

  .${widgetActionsMenuClass} {
    opacity: 1;
  }
`;

const GENERAL_TAB_VALUE = 'general';

const WelcomeMetrics = () => {
  const hasAccessToAnyStream = useHasAccessToAnyStream();
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

  if (!hasAccessToAnyStream) {
    return <NoStreamAccessAlert />;
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
