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
import React, { useMemo, useState } from 'react';
import * as Immutable from 'immutable';
import { MemoryRouter } from 'react-router-dom';
import type { Meta, StoryObj } from '@storybook/react-webpack5';

import { Button } from 'components/bootstrap';
import EntityDataTable from 'components/common/EntityDataTable';
import type { ColumnSchema } from 'components/common/EntityDataTable';
import CurrentUserContext from 'contexts/CurrentUserContext';
import User from 'logic/users/User';
import type { Sort } from 'stores/PaginationTypes';

// ─── Storybook-only setup ──────────────────────────────────────────────────
// EntityDataTable checks column permissions against the current user and reads the router location,
// so the stories provide a mock admin user and an in-memory router.

const storyUser = User.builder()
  .id('storybook-user')
  .username('admin')
  .fullName('Administrator')
  .firstName('Administrator')
  .lastName('')
  .email('admin@example.org')
  .permissions(Immutable.List(['*']))
  .grnPermissions(Immutable.List())
  .roles(Immutable.Set(['Admin']))
  .readOnly(false)
  .external(false)
  .sessionActive(true)
  .sessionTimeoutMs(10000000000)
  .clientAddress('127.0.0.1')
  .accountStatus('enabled')
  .startpage(null)
  .build();

// ─── Sample data ───────────────────────────────────────────────────────────

type Stream = {
  id: string;
  title: string;
  description: string;
  index_set: string;
  status: string;
};

const STREAMS: Array<Stream> = [
  {
    id: '1',
    title: 'Firewall events',
    description: 'Events from perimeter firewalls',
    index_set: 'Security',
    status: 'Running',
  },
  {
    id: '2',
    title: 'Authentication logs',
    description: 'Login attempts across all hosts',
    index_set: 'Security',
    status: 'Running',
  },
  {
    id: '3',
    title: 'Web access logs',
    description: 'Nginx and Apache access logs',
    index_set: 'Default',
    status: 'Paused',
  },
  {
    id: '4',
    title: 'Application errors',
    description: 'Errors reported by internal services',
    index_set: 'Default',
    status: 'Running',
  },
  {
    id: '5',
    title: 'DNS queries',
    description: 'Queries from internal resolvers',
    index_set: 'Network',
    status: 'Running',
  },
];

const COLUMN_SCHEMAS: Array<ColumnSchema> = [
  { id: 'title', title: 'Title', type: 'STRING', sortable: true },
  { id: 'description', title: 'Description', type: 'STRING', sortable: true },
  { id: 'index_set', title: 'Index Set', type: 'STRING', sortable: true },
  { id: 'status', title: 'Status', type: 'STRING', sortable: true },
];

const DEFAULT_COLUMNS = ['title', 'description', 'index_set', 'status'];

// ─── Interactive wrapper ───────────────────────────────────────────────────
// Keeps sort and layout in local state so sorting, column visibility and ordering work in Storybook.

type DemoProps = {
  withBulkSelection?: boolean;
  withRowActions?: boolean;
};

const renderRowActions = () => (
  <Button bsSize="xs" bsStyle="transparent">
    Edit
  </Button>
);

const EntityDataTableDemo = ({ withBulkSelection = false, withRowActions = true }: DemoProps) => {
  const [activeSort, setActiveSort] = useState<Sort>({ attributeId: 'title', direction: 'asc' });
  const [layoutPreferences, setLayoutPreferences] = useState<{
    attributes?: { [attributeId: string]: { status: 'show' | 'hide'; width?: number } };
    order?: Array<string>;
  }>({});

  const entities = useMemo(
    () =>
      [...STREAMS].sort((a, b) => {
        const result = String(a[activeSort.attributeId]).localeCompare(String(b[activeSort.attributeId]));

        return activeSort.direction === 'asc' ? result : -result;
      }),
    [activeSort],
  );

  return (
    <EntityDataTable<Stream>
      entities={entities}
      columnSchemas={COLUMN_SCHEMAS}
      defaultDisplayedColumns={DEFAULT_COLUMNS}
      defaultColumnOrder={DEFAULT_COLUMNS}
      entityAttributesAreCamelCase={false}
      activeSort={activeSort}
      onSortChange={setActiveSort}
      layoutPreferences={layoutPreferences}
      onLayoutPreferencesChange={async (prefs) => setLayoutPreferences((cur) => ({ ...cur, ...prefs }))}
      onResetLayoutPreferences={async () => setLayoutPreferences({})}
      onChangeSlicing={() => {}}
      noPageSizeSelect
      entityActions={withRowActions ? renderRowActions : undefined}
      bulkSelection={
        withBulkSelection
          ? {
              actions: (
                <Button bsSize="xs" bsStyle="danger">
                  Delete Selected
                </Button>
              ),
            }
          : undefined
      }
    />
  );
};

// ─── Stories ───────────────────────────────────────────────────────────────

const meta = {
  title: 'Components/EntityDataTable',
  component: EntityDataTableDemo,
  decorators: [
    (Story) => (
      <MemoryRouter>
        <CurrentUserContext.Provider value={storyUser}>
          <Story />
        </CurrentUserContext.Provider>
      </MemoryRouter>
    ),
  ],
  argTypes: {
    withRowActions: { control: 'boolean', description: 'Show an action button at the end of each row' },
    withBulkSelection: { control: 'boolean', description: 'Show row checkboxes and a bulk action' },
  },
} satisfies Meta<typeof EntityDataTableDemo>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {
  args: { withRowActions: true, withBulkSelection: false },
};

export const WithBulkSelection: Story = {
  args: { withRowActions: true, withBulkSelection: true },
};
