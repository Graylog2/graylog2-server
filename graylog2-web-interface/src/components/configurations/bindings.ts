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
import type { PluginExports } from 'graylog-web-plugin/plugin';

import ConfigurationSection from 'pages/configurations/ConfigurationSection';
import SearchesConfig from 'components/configurations/SearchesConfig';
import MessageProcessorsConfig from 'components/configurations/MessageProcessorsConfig';
import EventsConfig from 'components/configurations/EventsConfig';
import UrlAllowListConfig from 'components/configurations/UrlAllowListConfig';
import DecoratorsConfig from 'components/configurations/DecoratorsConfig';
import MarkdownConfig from 'components/configurations/MarkdownConfig';
import WelcomePageConfig from 'components/configurations/WelcomePageConfig';
import McpConfig from 'components/configurations/McpConfig';

const bindings: PluginExports = {
  coreSystemConfigurations: [
    {
      name: 'Search',
      feature: 'search',
      SectionComponent: ConfigurationSection,
      props: {
        ConfigurationComponent: SearchesConfig,
        title: 'Search',
      },
    },
    {
      name: 'Message Processors',
      feature: 'message_processors',
      SectionComponent: ConfigurationSection,
      props: {
        ConfigurationComponent: MessageProcessorsConfig,
        title: 'Message Processors',
      },
    },
    {
      name: 'Events',
      feature: 'events',
      SectionComponent: ConfigurationSection,
      props: {
        ConfigurationComponent: EventsConfig,
        title: 'Events',
      },
    },
    {
      name: 'URL Allowlist',
      feature: 'url_allowlist',
      permissions: ['urlallowlist:read'],
      SectionComponent: ConfigurationSection,
      props: {
        ConfigurationComponent: UrlAllowListConfig,
        title: 'URL allowlist',
      },
    },
    {
      name: 'Decorators',
      feature: 'decorators',
      SectionComponent: ConfigurationSection,
      props: {
        ConfigurationComponent: DecoratorsConfig,
        title: 'Decorators',
      },
    },
    {
      name: 'Markdown',
      feature: 'markdown',
      SectionComponent: ConfigurationSection,
      props: {
        ConfigurationComponent: MarkdownConfig,
        title: 'Markdown',
      },
    },
    {
      name: 'Welcome Page',
      feature: 'welcome_page',
      SectionComponent: ConfigurationSection,
      props: {
        ConfigurationComponent: WelcomePageConfig,
        title: 'Welcome Page',
      },
    },
    {
      name: 'MCP',
      feature: 'mcp',
      SectionComponent: ConfigurationSection,
      props: {
        ConfigurationComponent: McpConfig,
        title: 'MCP',
      },
    },
  ],
};

export default bindings;
