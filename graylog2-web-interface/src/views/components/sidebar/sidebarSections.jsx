// @flow strict
import * as React from 'react';

import { type ViewMetaData as ViewMetadata } from 'views/stores/ViewMetadataStore';

import ViewDescription from './description/ViewDescription';
import AddWidgetButton from './create/AddWidgetButton';
import HighlightingRules from './highlighting/HighlightingRules';


export type SidebarSectionProps = {
  children: React.Node,
  sidebarIsInline: boolean,
  queryId: string,
  results: any,
  toggleSidebar: () => void,
  viewMetadata: ViewMetadata,
};

export type SidebarSection = {
  key: string,
  title: string,
  icon: string,
  content: SidebarSectionProps => React.Node,
};

const sidebarSections: Array<SidebarSection> = [
  {
    key: 'viewDescription',
    title: 'Description',
    icon: 'info',
    content: ({ results, viewMetadata }: SidebarSectionProps) => <ViewDescription results={results} viewMetadata={viewMetadata} />,
  },
  {
    key: 'create',
    icon: 'plus',
    title: 'Create',
    content: ({ toggleSidebar, queryId, sidebarIsInline }: SidebarSectionProps) => (
      <AddWidgetButton queryId={queryId}
                       onClick={!sidebarIsInline ? toggleSidebar : () => {}} />
    ),
  },
  {
    key: 'highlighting',
    icon: 'paragraph',
    title: 'Highlighting',
    content: () => <HighlightingRules />,
  },
  {
    key: 'fieldList',
    icon: 'subscript',
    title: 'Fields',
    content: ({ children }: SidebarSectionProps) => children,
  },
];

export default sidebarSections;
