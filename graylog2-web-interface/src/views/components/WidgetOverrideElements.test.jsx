// @flow strict
import * as React from 'react';
import { cleanup, render, waitForElement } from '@testing-library/react';

import suppressConsole from 'helpers/suppressConsole';
import WidgetOverrideElements from './WidgetOverrideElements';

jest.mock('views/logic/withPluginEntities', () => x => x);

describe('WidgetOverrideElements', () => {
  afterEach(cleanup);
  it('renders original children if no elements are present', async () => {
    const { getByText } = render((
      <WidgetOverrideElements widgetOverrideElements={[]}>
        <span>Hello world!</span>
      </WidgetOverrideElements>
    ));
    await waitForElement(() => getByText('Hello world!'));
  });
  it('renders original children if element does not throw', async () => {
    const { getByText } = render((
      <WidgetOverrideElements widgetOverrideElements={[() => null]}>
        <span>Hello world!</span>
      </WidgetOverrideElements>
    ));
    await waitForElement(() => getByText('Hello world!'));
  });
  it('propagates thrown errors', async () => {
    const throwElement = () => {
      throw new Error('The dungeon collapses, you die!');
    };
    expect(() => render((
      <WidgetOverrideElements widgetOverrideElements={[throwElement]}>
        <span>Hello world!</span>
      </WidgetOverrideElements>
    ))).toThrowError('The dungeon collapses, you die!');
  });
  it('renders thrown component if element throws one', async () => {
    suppressConsole(async () => {
      const Component = () => <span>I was thrown!</span>;
      const ThrowElement = () => {
        throw Component;
      };
      const { getByText } = render((
        <WidgetOverrideElements widgetOverrideElements={[() => <ThrowElement />]}>
          <span>Hello world!</span>
        </WidgetOverrideElements>
      ));
      await waitForElement(() => getByText('I was thrown!'));
    });
  });
});
