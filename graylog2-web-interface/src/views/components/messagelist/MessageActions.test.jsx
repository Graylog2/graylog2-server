// @flow strict
import * as React from 'react';
import { cleanup, render } from 'wrappedTestingLibrary';
import * as Immutable from 'immutable';

import type { SearchesConfig } from 'components/search/SearchConfig';
import MessageActions from './MessageActions';

const searchConfig: SearchesConfig = {
  analysis_disabled_fields: [],
  query_time_range_limit: 'PT0S',
  relative_timerange_options: {},
  surrounding_filter_fields: [
    'somefield',
    'someotherfield',
  ],
  surrounding_timerange_options: {
    PT1S: '1 second',
    PT1M: 'Only a minute',
  },
};

describe('MessageActions', () => {
  afterEach(cleanup);

  const renderActions = (props = {}) => render((
    <MessageActions index="some-index"
                    id="some-id"
                    fields={{
                      timestamp: '2020-02-28T09:45:31.368Z',
                    }}
                    disabled={false}
                    disableSurroundingSearch={false}
                    disableTestAgainstStream={false}
                    decorationStats={{}}
                    showOriginal
                    toggleShowOriginal={() => {}}
                    streams={Immutable.List()}
                    searchConfig={searchConfig}
                    {...props} />
  ));

  it('renders surrounding search button', () => {
    const { getByText } = renderActions();
    expect(getByText('Show surrounding messages')).toBeTruthy();
  });

  it('does not render surrounding search button if `disableSurroundingSearch` is `true`', () => {
    const { queryByText } = renderActions({ disableSurroundingSearch: true });
    expect(queryByText('Show surrounding messages')).toBeNull();
  });
});
