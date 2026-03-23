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
import { render, screen, within } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

import { SystemInputs } from '@graylog/server-api';

import type { Attribute } from 'stores/PaginationTypes';
import { asMock } from 'helpers/mocking';

import InputDiagnosisRulesTab from './InputDiagnosisRulesTab';

jest.mock('@graylog/server-api', () => ({
  SystemInputs: {
    getPipelineRulesPage: jest.fn(),
    getStreamRulesPage: jest.fn(),
  },
}));

jest.mock(
  'components/streamrules/HumanReadableStreamRule',
  () =>
    ({ streamRule }: { streamRule: { field: string; value: string } }) => (
      <span>
        {streamRule.field} {streamRule.value}
      </span>
    ),
);

jest.mock('stores/inputs/StreamRulesInputsStore', () => ({
  StreamRulesInputsStore: {
    getInitialState: jest.fn(() => ({ inputs: [] })),
    listen: jest.fn(() => jest.fn()),
  },
}));

const commonAttributes: Attribute[] = [
  {
    id: 'id',
    title: 'id',
    type: 'OBJECT_ID',
    sortable: true,
    searchable: true,
    hidden: true,
    related_property: '',
    related_collection: '',
    related_identifier: '',
    related_display_fields: [],
    related_display_template: '',
    filterable: false,
    filter_options: [],
  },
];

const pipelineRulesResponse = {
  query: '',
  pagination: { total: 1, count: 1, page: 1, per_page: 20 },
  total: 0,
  sort: 'rule',
  order: 'asc' as const,
  elements: [
    {
      id: 'pipeline-id-1:rule-id-1',
      pipeline_id: 'pipeline-id-1',
      pipeline: 'Test Pipeline',
      rule_id: 'rule-id-1',
      rule: 'Test Rule',
      connected_streams: [{ id: 'stream-id-1', title: 'Test Stream' }],
    },
  ],
  attributes: [
    ...commonAttributes,
    {
      id: 'rule',
      title: 'Pipeline Rule',
      type: 'STRING' as Attribute['type'],
      sortable: true,
      searchable: false,
      hidden: false,
      related_property: '',
      related_collection: '',
      related_identifier: '',
      related_display_fields: [],
      related_display_template: '',
      filterable: false,
      filter_options: [],
    },
    {
      id: 'pipeline',
      title: 'Source pipeline',
      type: 'STRING' as Attribute['type'],
      sortable: true,
      searchable: false,
      hidden: false,
      related_property: '',
      related_collection: '',
      related_identifier: '',
      related_display_fields: [],
      related_display_template: '',
      filterable: false,
      filter_options: [],
    },
    {
      id: 'connected_streams',
      title: 'Connected streams',
      type: 'STRING' as Attribute['type'],
      sortable: true,
      searchable: false,
      hidden: false,
      related_property: '',
      related_collection: '',
      related_identifier: '',
      related_display_fields: [],
      related_display_template: '',
      filterable: false,
      filter_options: [],
    },
  ],
  defaults: { sort: { id: 'rule', direction: 'ASC' as const } },
};

const streamRulesResponse = {
  query: '',
  pagination: { total: 1, count: 1, page: 1, per_page: 20 },
  total: 0,
  sort: 'stream',
  order: 'asc' as const,
  elements: [
    {
      id: 'stream-rule-id-1',
      stream_id: 'stream-id-1',
      stream: 'Test Stream',
      rule_field: 'gl2_source_input',
      rule_type: 1,
      rule_value: 'test-input-id',
      inverted: false,
      description: 'Route from input',
    },
  ],
  attributes: [
    ...commonAttributes,
    {
      id: 'stream',
      title: 'Stream',
      type: 'STRING' as Attribute['type'],
      sortable: true,
      searchable: false,
      hidden: false,
      related_property: '',
      related_collection: '',
      related_identifier: '',
      related_display_fields: [],
      related_display_template: '',
      filterable: false,
      filter_options: [],
    },
    {
      id: 'rule',
      title: 'Rule',
      type: 'STRING' as Attribute['type'],
      sortable: false,
      searchable: false,
      hidden: false,
      related_property: '',
      related_collection: '',
      related_identifier: '',
      related_display_fields: [],
      related_display_template: '',
      filterable: false,
      filter_options: [],
    },
    {
      id: 'rule_field',
      title: 'Rule field',
      type: 'STRING' as Attribute['type'],
      sortable: true,
      searchable: false,
      hidden: true,
      related_property: '',
      related_collection: '',
      related_identifier: '',
      related_display_fields: [],
      related_display_template: '',
      filterable: false,
      filter_options: [],
    },
    {
      id: 'rule_type',
      title: 'Rule type',
      type: 'STRING' as Attribute['type'],
      sortable: true,
      searchable: false,
      hidden: true,
      related_property: '',
      related_collection: '',
      related_identifier: '',
      related_display_fields: [],
      related_display_template: '',
      filterable: false,
      filter_options: [],
    },
    {
      id: 'rule_value',
      title: 'Rule value',
      type: 'STRING' as Attribute['type'],
      sortable: true,
      searchable: false,
      hidden: true,
      related_property: '',
      related_collection: '',
      related_identifier: '',
      related_display_fields: [],
      related_display_template: '',
      filterable: false,
      filter_options: [],
    },
  ],
  defaults: { sort: { id: 'stream', direction: 'ASC' as const } },
};

describe('<InputDiagnosisRulesTab />', () => {
  beforeEach(() => {
    asMock(SystemInputs.getPipelineRulesPage).mockReturnValue(Promise.resolve(pipelineRulesResponse) as any);
    asMock(SystemInputs.getStreamRulesPage).mockReturnValue(Promise.resolve(streamRulesResponse) as any);
  });

  it('should render pipeline rules', async () => {
    render(<InputDiagnosisRulesTab inputId="test-input-id" />);

    const row = await screen.findByTestId('table-row-pipeline-id-1:rule-id-1');

    expect(within(row).getByText(/Test Rule/i)).toBeInTheDocument();
    expect(within(row).getByText(/Test Pipeline/i)).toBeInTheDocument();
    expect(within(row).getByText(/Test Stream/i)).toBeInTheDocument();
  });

  it('should render stream rules with human readable rule', async () => {
    render(<InputDiagnosisRulesTab inputId="test-input-id" />);

    const row = await screen.findByTestId('table-row-stream-rule-id-1');

    expect(within(row).getByText(/Test Stream/i)).toBeInTheDocument();
    expect(within(row).getByText(/gl2_source_input test-input-id/i)).toBeInTheDocument();
  });

  it('keeps both table search inputs independent', async () => {
    render(<InputDiagnosisRulesTab inputId="test-input-id" />);

    const pipelineSearch = await screen.findByPlaceholderText('Search for pipeline rule');
    const streamSearch = await screen.findByPlaceholderText('Search for stream rule');

    await userEvent.type(pipelineSearch, 'pipeline filter');

    expect(pipelineSearch).toHaveValue('pipeline filter');
    expect(streamSearch).toHaveValue('');

    await userEvent.type(streamSearch, 'stream filter');

    expect(pipelineSearch).toHaveValue('pipeline filter');
    expect(streamSearch).toHaveValue('stream filter');
  });
});
