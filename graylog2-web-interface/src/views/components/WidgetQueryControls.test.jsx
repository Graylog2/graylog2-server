// @flow strict
import * as React from 'react';
import { render, waitForElement, cleanup, fireEvent } from '@testing-library/react';

import { GlobalOverrideActions } from 'views/stores/GlobalOverrideStore';
import WidgetQueryControls from './WidgetQueryControls';
import Widget from '../logic/widgets/Widget';

jest.mock('views/stores/WidgetStore', () => ({
  WidgetActions: {},
}));
jest.mock('views/stores/GlobalOverrideStore', () => ({
  GlobalOverrideActions: {
    reset: jest.fn(),
  },
}));
jest.mock('stores/connect', () => x => x);

describe('WidgetQueryControls', () => {
  afterEach(cleanup);

  const config = { relative_timerange_options: {}, query_time_range_limit: '' };
  const renderSUT = (props) => render(
    <WidgetQueryControls {...props}
                         widget={
                           Widget.builder()
                             .type('foo')
                             .build()
                         }
                         availableStreams={[]}
                         config={config} />,
  );
  it('should do something', () => {
    const { container } = renderSUT();
    expect(container).toMatchSnapshot();
  });

  it('shows an indicator if global override is set', async () => {
    const { getByText, getByTestId } = renderSUT({
      globalOverride: {
        query: {
          type: 'elasticsearch',
          query_string: 'source:foo',
        },
      },
    });
    await waitForElement(() => getByText('These controls are disabled because a filter is applied to all widgets.'));
    await waitForElement(() => getByTestId('reset-filter'));
  });

  it('does not show an indicator if global override is not set', async () => {
    const { queryByText } = renderSUT({ globalOverride: {} });
    expect(queryByText('These controls are disabled because a filter is applied to all widgets.')).toBeNull();
  });
  
  it('reset filter button triggers resetting the global override store', async () => {
    const { getByTestId } = renderSUT({
      globalOverride: {
        query: {
          type: 'elasticsearch',
          query_string: 'source:foo',
        },
      },
    });
    const resetFilterButton = await waitForElement(() => getByTestId('reset-filter'));
    fireEvent.click(resetFilterButton);
    expect(GlobalOverrideActions.reset).toHaveBeenCalled();
  });
});
