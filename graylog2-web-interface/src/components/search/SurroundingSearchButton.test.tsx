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
import { asElement, fireEvent, render } from 'wrappedTestingLibrary';

import DrilldownContext from 'views/components/contexts/DrilldownContext';

import SurroundingSearchButton from './SurroundingSearchButton';
import type { SearchesConfig } from './SearchConfig';

const getOption = (optionText, getByText) => {
  const button = getByText('Show surrounding messages');

  fireEvent.click(button);

  return getByText(optionText);
};

describe('SurroundingSearchButton', () => {
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
  const TestComponent = (props) => (
    <SurroundingSearchButton searchConfig={searchConfig}
                             timestamp="2020-02-28T09:45:31.123Z"
                             id="foo-bar"
                             messageFields={{}}
                             {...props} />
  );

  const renderButton = (props = {}) => render(<TestComponent {...props} />);

  it('renders a button with a "Show surrounding messages" caption', () => {
    const { getByText } = renderButton();

    expect(getByText('Show surrounding messages')).toBeTruthy();
  });

  it('shows a dropdown after clicking the button', () => {
    const { getByText } = renderButton();
    const button = getByText('Show surrounding messages');

    fireEvent.click(button);

    expect(getByText('Only a minute')).toBeTruthy();
    expect(getByText('1 second')).toBeTruthy();
  });

  it('one second option has a valid url', () => {
    const { getByText } = renderButton();

    const oneSecond = getOption('1 second', getByText);

    expect(asElement(oneSecond, HTMLAnchorElement).href).toEqual(
      'http://localhost/search?rangetype=absolute&from=2020-02-28T09%3A45%3A30.123Z&to=2020-02-28T09%3A45%3A32.123Z&highlightMessage=foo-bar',
    );
  });

  it('"Only a minute" option has a valid url', () => {
    const { getByText } = renderButton();

    const onlyAMinute = asElement(getOption('Only a minute', getByText), HTMLAnchorElement);

    expect(onlyAMinute.href).toEqual(
      'http://localhost/search?rangetype=absolute&from=2020-02-28T09%3A44%3A31.123Z&to=2020-02-28T09%3A46%3A31.123Z&highlightMessage=foo-bar',
    );
  });

  it('the option opens the link in a new page', () => {
    const { getByText } = renderButton();

    const option = asElement(getOption('1 second', getByText), HTMLAnchorElement);

    expect(option.target).toEqual('_blank');
    expect(option.rel).toEqual('noopener noreferrer');
  });

  it('the "1 second" option has a valid url', () => {
    const { getByText } = renderButton({
      messageFields: {
        somefield: '42',
      },
    });

    const option = asElement(getOption('1 second', getByText), HTMLAnchorElement);

    expect(option.href).toContain('q=somefield%3A%2242%22');
    expect(option.href).not.toContain('someotherfield');
  });

  it('adds a query parameter for highlighting the id of the current message', () => {
    const { getByText } = renderButton();

    const option = asElement(getOption('1 second', getByText), HTMLAnchorElement);

    expect(option.href).toContain('highlightMessage=foo-bar');
  });

  it('includes current set of streams in generated urls', () => {
    const streams = ['000000000000000000000001', '5c2e07eeba33a9681ad6070a', '5d2d9649e117dc4df84cf83c'];
    const { getByText } = render((
      <DrilldownContext.Consumer>
        {(drilldown) => (
          <DrilldownContext.Provider value={{ ...drilldown, streams }}>
            <TestComponent />
          </DrilldownContext.Provider>
        )}
      </DrilldownContext.Consumer>
    ));

    const option = asElement(getOption('1 second', getByText), HTMLAnchorElement);

    expect(option.href).toContain('streams=000000000000000000000001%2C5c2e07eeba33a9681ad6070a%2C5d2d9649e117dc4df84cf83c');
  });

  it('does not include a `streams` key in generated urls if none are selected', () => {
    const { getByText } = renderButton();

    const option = asElement(getOption('1 second', getByText), HTMLAnchorElement);

    expect(option.href).not.toContain('streams=');
  });
});
