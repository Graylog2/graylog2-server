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
import { asElement, fireEvent, render, screen } from 'wrappedTestingLibrary';

import DrilldownContext from 'views/components/contexts/DrilldownContext';

import SurroundingSearchButton from './SurroundingSearchButton';

const getOption = async (optionText: string) => {
  const button = await screen.findByRole('button', { name: /show surrounding messages/i });

  fireEvent.click(button);

  return screen.findByRole('menuitem', { name: new RegExp(optionText, 'i') });
};

describe('SurroundingSearchButton', () => {
  const searchConfig = {
    surrounding_filter_fields: [
      'somefield',
      'someotherfield',
    ],
    surrounding_timerange_options: {
      PT1S: '1 second',
      PT1M: 'Only a minute',
    },
  };
  const TestComponent = (props: Partial<React.ComponentProps<typeof SurroundingSearchButton>>) => (
    <SurroundingSearchButton searchConfig={searchConfig}
                             timestamp="2020-02-28T09:45:31.123Z"
                             id="foo-bar"
                             messageFields={{}}
                             {...props} />
  );

  const renderButton = (props: Partial<React.ComponentProps<typeof SurroundingSearchButton>> = {}) => render(<TestComponent {...props} />);

  it('renders a button with a "Show surrounding messages" caption', () => {
    renderButton();

    expect(screen.getByText('Show surrounding messages')).toBeTruthy();
  });

  it('shows a dropdown after clicking the button', async () => {
    renderButton();
    const button = screen.getByText('Show surrounding messages');

    fireEvent.click(button);

    await screen.findByText('Only a minute');
    await screen.findByText('1 second');
  });

  it('one second option has a valid url', async () => {
    renderButton();

    const oneSecond = await getOption('1 second');

    expect(asElement(oneSecond, HTMLAnchorElement).href).toEqual(
      'http://localhost/search?rangetype=absolute&from=2020-02-28T09%3A45%3A30.123Z&to=2020-02-28T09%3A45%3A32.123Z&highlightMessage=foo-bar',
    );
  });

  it('"Only a minute" option has a valid url', async () => {
    renderButton();

    const onlyAMinute = asElement(await getOption('Only a minute'), HTMLAnchorElement);

    expect(onlyAMinute.href).toEqual(
      'http://localhost/search?rangetype=absolute&from=2020-02-28T09%3A44%3A31.123Z&to=2020-02-28T09%3A46%3A31.123Z&highlightMessage=foo-bar',
    );
  });

  it('the option opens the link in a new page', async () => {
    renderButton();

    const option = asElement(await getOption('1 second'), HTMLAnchorElement);

    expect(option.target).toEqual('_blank');
    expect(option.rel).toEqual('noopener noreferrer');
  });

  it('the "1 second" option has a valid url', async () => {
    renderButton({
      messageFields: {
        somefield: '42',
      },
    });

    const option = asElement(await getOption('1 second'), HTMLAnchorElement);

    expect(option.href).toContain('q=somefield%3A%2242%22');
    expect(option.href).not.toContain('someotherfield');
  });

  it('adds a query parameter for highlighting the id of the current message', async () => {
    renderButton();

    const option = asElement(await getOption('1 second'), HTMLAnchorElement);

    expect(option.href).toContain('highlightMessage=foo-bar');
  });

  it('includes current set of streams in generated urls', async () => {
    const streams = ['000000000000000000000001', '5c2e07eeba33a9681ad6070a', '5d2d9649e117dc4df84cf83c'];

    render((
      <DrilldownContext.Consumer>
        {(drilldown) => (
          <DrilldownContext.Provider value={{ ...drilldown, streams }}>
            <TestComponent />
          </DrilldownContext.Provider>
        )}
      </DrilldownContext.Consumer>
    ));

    const option = asElement(await getOption('1 second'), HTMLAnchorElement);

    expect(option.href).toContain('streams=000000000000000000000001%2C5c2e07eeba33a9681ad6070a%2C5d2d9649e117dc4df84cf83c');
  });

  it('does not include a `streams` key in generated urls if none are selected', async () => {
    renderButton();

    const option = asElement(await getOption('1 second'), HTMLAnchorElement);

    expect(option.href).not.toContain('streams=');
  });
});
