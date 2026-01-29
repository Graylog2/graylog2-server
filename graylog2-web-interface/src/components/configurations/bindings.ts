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
import SidecarConfig from 'components/configurations/SidecarConfig';
import EventsConfig from 'components/configurations/EventsConfig';
import UrlAllowListConfig from 'components/configurations/UrlAllowListConfig';
import DecoratorsConfig from 'components/configurations/DecoratorsConfig';
import PermissionsConfig from 'components/configurations/PermissionsConfig';
import UserConfig from 'components/configurations/UserConfig';
import MarkdownConfig from 'components/configurations/MarkdownConfig';
import McpConfig from 'components/configurations/McpConfig';
import PasswordComplexityConfig from 'components/configurations/PasswordComplexityConfig';

const bindings: PluginExports = {
  coreSystemConfigurations: [
    {
      name: 'Search',
      SectionComponent: ConfigurationSection,
      props: {
        ConfigurationComponent: SearchesConfig,
        title: 'Search',
      },
    },
    {
      name: 'Message Processors',
      SectionComponent: ConfigurationSection,
      props: {
        ConfigurationComponent: MessageProcessorsConfig,
        title: 'Message Processors',
      },
    },
    {
      name: 'Sidecars',
      SectionComponent: ConfigurationSection,
      props: {
        ConfigurationComponent: SidecarConfig,
        title: 'Sidecars',
      },
    },
    {
      name: 'Events',
      SectionComponent: ConfigurationSection,
      props: {
        ConfigurationComponent: EventsConfig,
        title: 'Events',
      },
    },
    {
      name: 'URL Allowlist',
      permissions: ['urlallowlist:read'],
      SectionComponent: ConfigurationSection,
      props: {
        ConfigurationComponent: UrlAllowListConfig,
        title: 'URL allowlist',
      },
    },
    {
      name: 'Decorators',
      SectionComponent: ConfigurationSection,
      props: {
        ConfigurationComponent: DecoratorsConfig,
        title: 'Decorators',
      },
    },
    {
      name: 'Permissions',
      SectionComponent: ConfigurationSection,
      props: {
        ConfigurationComponent: PermissionsConfig,
        title: 'Permissions',
      },
    },
    {
      name: 'Password Policy',
      SectionComponent: ConfigurationSection,
      props: {
        ConfigurationComponent: PasswordComplexityConfig,
        title: 'Password Policy',
      },
    },
    {
      name: 'Users',
      SectionComponent: ConfigurationSection,
      props: {
        ConfigurationComponent: UserConfig,
        title: 'Index Set Defaults',
      },
    },
    {
      name: 'Markdown',
      SectionComponent: ConfigurationSection,
      props: {
        ConfigurationComponent: MarkdownConfig,
        title: 'Markdown',
      },
    },
    {
      name: 'MCP',
      SectionComponent: ConfigurationSection,
      props: {
        ConfigurationComponent: McpConfig,
        title: 'MCP',
      },
    },
  ],
};

export default bindings;
