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

import { DocumentTitle, PageHeader } from 'components/common';
import EventsPageNavigation from 'components/events/EventsPageNavigation';
import usePluginEntities from 'hooks/usePluginEntities';
import useLocation from 'routing/useLocation';

const PAGE_TITLE_BY_ACTION: Record<string, string> = {
  'git-import': 'Create Event Definitions - Git Import',
  'file-import': 'Create Event Definitions - File Import',
};

const PLUGIN_KEY_BY_ACTION: Record<
  string,
  'eventDefinitions.components.sigmaGitImport' | 'eventDefinitions.components.sigmaFileUpload'
> = {
  'git-import': 'eventDefinitions.components.sigmaGitImport',
  'file-import': 'eventDefinitions.components.sigmaFileUpload',
};

function SigmaEventDefinitionPage() {
  const { pathname } = useLocation();
  const pathAction = pathname.split('/').pop() ?? '';
  const pluginKey = PLUGIN_KEY_BY_ACTION[pathAction];
  const pluginComponents = usePluginEntities(pluginKey);
  const PluginComponent = pluginComponents?.[0]?.component;

  if (!PluginComponent) {
    return null;
  }

  const pageTitle = PAGE_TITLE_BY_ACTION[pathAction] ?? 'Create Event Definitions';

  return (
    <DocumentTitle title={pageTitle}>
      <EventsPageNavigation />
      <PageHeader title={pageTitle}>
        <span>Event Definitions allow you to create Alerts from different Conditions and alert on them.</span>
      </PageHeader>
      <PluginComponent />
    </DocumentTitle>
  );
}

export default SigmaEventDefinitionPage;
