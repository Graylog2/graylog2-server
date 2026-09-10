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

import usePluggableLicenseCheck from 'hooks/usePluggableLicenseCheck';
import usePluginEntities from 'hooks/usePluginEntities';
import { SegmentedControl } from 'components/bootstrap';
import { widgetActionsMenuClass } from 'views/components/widgets/Constants';
import SectionHeader from 'components/welcome/SectionHeader';
import GeneralMetrics from 'components/welcome/GeneralMetrics';
import type { WelcomePageMetricsPlugin } from 'components/welcome/types';
import useHasAccessToAnyStream from 'hooks/useHasAccessToAnyStream';
import NoStreamAccessAlert from 'components/welcome/NoStreamAccessAlert';

import GeneralWelcomeMetrics from './GeneralWelcomeMetrics';

const Container = styled.div`
  margin-bottom: 6.4px;

  .${widgetActionsMenuClass} {
    opacity: 1;
  }
`;

const GENERAL_TAB_VALUE = 'general';

type OverviewSectionProps = React.PropsWithChildren<{
  headerActions?: React.ReactNode;
}>;

const OverviewSection = ({ headerActions = undefined, children = undefined }: OverviewSectionProps) => (
  <>
    <SectionHeader>
      <h2>Overview</h2>
      {headerActions}
    </SectionHeader>
    <Container>{children}</Container>
  </>
);

const MetricsTabs = ({ metricsPlugins }: { metricsPlugins: Array<WelcomePageMetricsPlugin> }) => {
  const [selectedTabValue, setSelectedTabValue] = useState<string>();

  const tabs = [
    { label: 'General', value: GENERAL_TAB_VALUE, component: GeneralMetrics },
    ...metricsPlugins.map(({ label, component }) => ({ label, value: label, component })),
  ];
  const activeTabValue = selectedTabValue ?? metricsPlugins[0].label;
  const ActiveTabComponent = tabs.find(({ value }) => value === activeTabValue)?.component ?? GeneralMetrics;

  return (
    <OverviewSection
      headerActions={
        <SegmentedControl
          data={tabs.map(({ label, value }) => ({ label, value }))}
          value={activeTabValue}
          onChange={setSelectedTabValue}
        />
      }>
      <ActiveTabComponent />
    </OverviewSection>
  );
};

const WelcomeMetricsContent = () => {
  const {
    data: { valid: isValidSecurityLicense },
  } = usePluggableLicenseCheck('/license/security');
  const pluginContext = { isValidSecurityLicense };

  const welcomePageMetricsPlugins = usePluginEntities('welcomePageMetrics');
  const enabledWelcomePageMetricsPlugins = welcomePageMetricsPlugins.filter((plugin) =>
    typeof plugin.isEnabled === 'function' ? plugin.isEnabled(pluginContext) : true,
  );

  if (enabledWelcomePageMetricsPlugins.length > 0) {
    return <MetricsTabs metricsPlugins={enabledWelcomePageMetricsPlugins} />;
  }

  return (
    <OverviewSection>
      <GeneralWelcomeMetrics />
    </OverviewSection>
  );
};

const WelcomeMetricsSection = () => {
  const { hasAccessToAnyStream } = useHasAccessToAnyStream();

  if (!hasAccessToAnyStream) {
    return <NoStreamAccessAlert />;
  }

  return <WelcomeMetricsContent />;
};

export default WelcomeMetricsSection;
