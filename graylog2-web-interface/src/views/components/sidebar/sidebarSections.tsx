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

import i18n from 'i18n';
import type { IconName } from 'components/common/Icon';
import ViewsHighlightingRules from 'views/components/sidebar/highlighting/ViewsHighlightingRules';

import ViewDescription from './description/ViewDescription';
import AddWidgetButton from './create/AddWidgetButton';

export type SidebarSectionProps = {
  sidebarChildren: React.ReactElement;
  sidebarIsPinned: boolean;
  results: any;
  toggleSidebar: () => void;
};

export type SidebarSection = {
  key: string;
  title: string | null;
  hoverTitle?: string;
  icon: IconName;
  content: React.ComponentType<SidebarSectionProps>;
};

const sidebarSections: Array<SidebarSection> = [
  {
    key: 'viewDescription',
    get title() {
      return i18n.t('sidebar.description', { ns: 'search' });
    },
    icon: 'info',
    content: ({ results }: SidebarSectionProps) => <ViewDescription results={results} />,
  },
  {
    key: 'create',
    icon: 'add',
    get title() {
      return i18n.t('sidebar.create', { ns: 'search' });
    },
    content: ({ toggleSidebar, sidebarIsPinned }: SidebarSectionProps) => (
      <AddWidgetButton onClick={!sidebarIsPinned ? toggleSidebar : () => {}} />
    ),
  },
  {
    key: 'highlighting',
    icon: 'format_paragraph',
    get title() {
      return i18n.t('sidebar.highlighting', { ns: 'search' });
    },
    content: () => <ViewsHighlightingRules />,
  },
  {
    key: 'fieldList',
    icon: 'subscript',
    get title() {
      return i18n.t('sidebar.fields', { ns: 'search' });
    },
    content: ({ sidebarChildren }: SidebarSectionProps) => sidebarChildren,
  },
];

export default sidebarSections;
