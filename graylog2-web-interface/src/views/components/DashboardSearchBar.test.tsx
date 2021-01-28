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
import { StoreMock as MockStore } from 'helpers/mocking';

import DashboardSearchBar from './DashboardSearchBar';

jest.mock('views/components/ViewActionsMenu', () => () => <span>View Actions</span>);

jest.mock('views/stores/GlobalOverrideStore', () => ({
  GlobalOverrideActions: {
    set: jest.fn(() => Promise.resolve()),
  },
  GlobalOverrideStore: MockStore(),
}));

const config = {
  analysis_disabled_fields: ['full_message', 'message'],
  query_time_range_limit: 'PT0S',
  relative_timerange_options: { PT0S: 'Search in all messages', P5m: 'Search in last five minutes' },
  surrounding_filter_fields: ['file', 'source', 'gl2_source_input', 'source_file'],
  surrounding_timerange_options: { PT1S: 'One second', PT2S: 'Two seconds' },
};

describe('DashboardSearchBar', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  const onExecute = jest.fn();

  it('defaults to no override being selected', () => {
    render(<DashboardSearchBar onExecute={onExecute} config={config} />);

    expect(screen.getByText('No Override')).toBeVisible();
  });
});
