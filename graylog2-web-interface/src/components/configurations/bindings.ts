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

import i18n from 'i18n';

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
import WelcomePageConfig from 'components/configurations/WelcomePageConfig';
import McpConfig from 'components/configurations/McpConfig';
import PasswordComplexityConfig from 'components/configurations/PasswordComplexityConfig';

const bindings: PluginExports = {
  coreSystemConfigurations: [
    {
      name: 'Search',
      // Read lazily, so the label is translated when the page renders and not when the module loads.
      get displayName() {
        return i18n.t('sections.search', { ns: 'configurations' });
      },
      SectionComponent: ConfigurationSection,
      props: {
        ConfigurationComponent: SearchesConfig,
        get title() {
          return i18n.t('sections.search', { ns: 'configurations' });
        },
      },
    },
    {
      name: 'Message Processors',
      // Read lazily, so the label is translated when the page renders and not when the module loads.
      get displayName() {
        return i18n.t('sections.message_processors', { ns: 'configurations' });
      },
      SectionComponent: ConfigurationSection,
      props: {
        ConfigurationComponent: MessageProcessorsConfig,
        get title() {
          return i18n.t('sections.message_processors', { ns: 'configurations' });
        },
      },
    },
    {
      name: 'Sidecars',
      // Read lazily, so the label is translated when the page renders and not when the module loads.
      get displayName() {
        return i18n.t('sections.sidecars', { ns: 'configurations' });
      },
      SectionComponent: ConfigurationSection,
      props: {
        ConfigurationComponent: SidecarConfig,
        get title() {
          return i18n.t('sections.sidecars', { ns: 'configurations' });
        },
      },
    },
    {
      name: 'Events',
      // Read lazily, so the label is translated when the page renders and not when the module loads.
      get displayName() {
        return i18n.t('sections.events', { ns: 'configurations' });
      },
      SectionComponent: ConfigurationSection,
      props: {
        ConfigurationComponent: EventsConfig,
        get title() {
          return i18n.t('sections.events', { ns: 'configurations' });
        },
      },
    },
    {
      name: 'URL Allowlist',
      // Read lazily, so the label is translated when the page renders and not when the module loads.
      get displayName() {
        return i18n.t('sections.url_allowlist', { ns: 'configurations' });
      },
      permissions: ['urlallowlist:read'],
      SectionComponent: ConfigurationSection,
      props: {
        ConfigurationComponent: UrlAllowListConfig,
        get title() {
          return i18n.t('sections.url_allowlist_title', { ns: 'configurations' });
        },
      },
    },
    {
      name: 'Decorators',
      // Read lazily, so the label is translated when the page renders and not when the module loads.
      get displayName() {
        return i18n.t('sections.decorators', { ns: 'configurations' });
      },
      SectionComponent: ConfigurationSection,
      props: {
        ConfigurationComponent: DecoratorsConfig,
        get title() {
          return i18n.t('sections.decorators', { ns: 'configurations' });
        },
      },
    },
    {
      name: 'Permissions',
      // Read lazily, so the label is translated when the page renders and not when the module loads.
      get displayName() {
        return i18n.t('sections.permissions', { ns: 'configurations' });
      },
      SectionComponent: ConfigurationSection,
      props: {
        ConfigurationComponent: PermissionsConfig,
        get title() {
          return i18n.t('sections.permissions', { ns: 'configurations' });
        },
      },
    },
    {
      name: 'Password Policy',
      // Read lazily, so the label is translated when the page renders and not when the module loads.
      get displayName() {
        return i18n.t('sections.password_policy', { ns: 'configurations' });
      },
      SectionComponent: ConfigurationSection,
      props: {
        ConfigurationComponent: PasswordComplexityConfig,
        get title() {
          return i18n.t('sections.password_policy', { ns: 'configurations' });
        },
      },
    },
    {
      name: 'Users',
      // Read lazily, so the label is translated when the page renders and not when the module loads.
      get displayName() {
        return i18n.t('sections.users', { ns: 'configurations' });
      },
      SectionComponent: ConfigurationSection,
      props: {
        ConfigurationComponent: UserConfig,
        get title() {
          return i18n.t('sections.users_title', { ns: 'configurations' });
        },
      },
    },
    {
      name: 'Markdown',
      // Read lazily, so the label is translated when the page renders and not when the module loads.
      get displayName() {
        return i18n.t('sections.markdown', { ns: 'configurations' });
      },
      SectionComponent: ConfigurationSection,
      props: {
        ConfigurationComponent: MarkdownConfig,
        get title() {
          return i18n.t('sections.markdown', { ns: 'configurations' });
        },
      },
    },
    {
      name: 'Welcome Page',
      // Read lazily, so the label is translated when the page renders and not when the module loads.
      get displayName() {
        return i18n.t('sections.welcome_page', { ns: 'configurations' });
      },
      SectionComponent: ConfigurationSection,
      props: {
        ConfigurationComponent: WelcomePageConfig,
        get title() {
          return i18n.t('sections.welcome_page', { ns: 'configurations' });
        },
      },
    },
    {
      name: 'MCP',
      // Read lazily, so the label is translated when the page renders and not when the module loads.
      get displayName() {
        return i18n.t('sections.mcp', { ns: 'configurations' });
      },
      SectionComponent: ConfigurationSection,
      props: {
        ConfigurationComponent: McpConfig,
        get title() {
          return i18n.t('sections.mcp', { ns: 'configurations' });
        },
      },
    },
  ],
};

export default bindings;
