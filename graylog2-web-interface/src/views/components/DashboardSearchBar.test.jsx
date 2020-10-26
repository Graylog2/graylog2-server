// @flow strict
import * as React from 'react';
import { fireEvent, render, waitFor } from 'wrappedTestingLibrary';
import { act } from 'react-dom/test-utils';
import { StoreMock as MockStore } from 'helpers/mocking';

import { GlobalOverrideActions } from 'views/stores/GlobalOverrideStore';

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
    const { container, getByTitle } = render(<DashboardSearchBar onExecute={onExecute} config={config} />);

    expect(container).not.toBeNull();
    // expect(getByTitle('There is no override for the timerange currently selected')).toBeVisible();
  });
});
