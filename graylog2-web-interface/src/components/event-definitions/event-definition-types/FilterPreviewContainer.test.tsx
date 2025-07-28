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
import { render, screen } from 'wrappedTestingLibrary';

import type { EventDefinition } from 'components/event-definitions/event-definitions-types';
import type { Stream } from 'logic/streams/types';
import StreamsContext from 'contexts/StreamsContext';

import FilterPreviewContainer from './FilterPreviewContainer';

const eventDefinition: EventDefinition = {
  id: 'deadbeef',
  title: 'Sample Event Definition',
  description: 'Sample Event Definition',
  _scope: 'test',
  priority: 1,
  alert: true,
  config: {
    type: 'foo',
    query: '*',
    query_parameters: [],
    filters: [],
    streams: [],
    group_by: [],
    _is_scheduled: true,
    series: [],
    conditions: {
      expression: {},
    },
    search_within_ms: 300,
    execute_every_ms: 300,
    event_limit: 10,
  },
  field_spec: {},
  key_spec: [],
  notification_settings: {
    grace_period_ms: 300,
    backlog_size: 10,
  },
  notifications: [],
  storage: [],
  updated_at: null,
  state: 'ENABLED',
  remediation_steps: '',
  event_procedure: '',
  matched_at: '',
  scheduler: {
    data: {
      type: '',
      timerange_from: 0,
      timerange_to: 0,
    },
    next_time: '',
    triggered_at: '',
    queued_notifications: 0,
    is_scheduled: false,
    status: 'runnable',
  },
};

const SUT = ({ streams = [] }: { streams?: Array<Stream> }) => (
  <StreamsContext.Provider value={streams}>
    <FilterPreviewContainer eventDefinition={eventDefinition} />
  </StreamsContext.Provider>
);

jest.mock('./FilterPreview', () => () => <span>Filter results</span>);

describe('FilterPreviewContainer', () => {
  it('does not execute search but shows message if user does not has access to any stream', async () => {
    render(<SUT />);
    await screen.findByText(/Unable to preview filter, user does not have access to any streams./i);
  });

  it('executes search if user has access to at least one stream', async () => {
    const streams: Array<Stream> = [
      {
        id: 'stream1',
        creator_user_id: 'admin',
        outputs: [],
        matching_type: 'AND',
        title: 'Test Stream',
        description: 'Test Stream',
        created_at: 'now',
        disabled: false,
        rules: [],
        content_pack: null,
        remove_matches_from_default_stream: false,
        index_set_id: 'index-set-1',
        is_default: false,
        is_editable: true,
        categories: [],
      },
    ];
    render(<SUT streams={streams} />);
    await screen.findByText(/Filter results/i);
  });
});
