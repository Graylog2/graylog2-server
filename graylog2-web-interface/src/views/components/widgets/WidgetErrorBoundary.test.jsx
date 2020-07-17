// @flow strict
import * as React from 'react';
import { render } from 'wrappedTestingLibrary';
import asMock from 'helpers/mocking/AsMock';

import WidgetErrorBoundary from './WidgetErrorBoundary';

describe('WidgetErrorBoundary', () => {
  it('renders children if no error is thrown', async () => {
    const { findByText } = render((
      <WidgetErrorBoundary>
        <div>Hello World!</div>
      </WidgetErrorBoundary>
    ));

    await findByText('Hello World!');
  });

  it('renders helpful error message if child throws error', async () => {
    jest.spyOn(console, 'error');
    // eslint-disable-next-line no-console
    asMock(console.error).mockImplementation(jest.fn());

    const Component = () => { throw new Error('The dungeon collapses, you die!'); };
    const { findByText } = render((
      <WidgetErrorBoundary>
        <Component />
      </WidgetErrorBoundary>
    ));

    await findByText('While rendering this widget, the following error occurred:');
    await findByText('Error: The dungeon collapses, you die!');

    // eslint-disable-next-line no-console
    asMock(console.error).mockRestore();
  });

  it('passes own props to its children', () => {
    const Component = (props) => <div data-testid="child-component-test-id" {...props} />;
    const { getByTestId } = render((
      <WidgetErrorBoundary extraProp="The extra prop">
        <Component />
      </WidgetErrorBoundary>
    ));

    expect(getByTestId('child-component-test-id')).toHaveAttribute('extraProp', 'The extra prop');
  });
});
