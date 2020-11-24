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
import React, { useEffect } from 'react';
import { render } from 'wrappedTestingLibrary';
import suppressConsole from 'helpers/suppressConsole';

import WidgetOverrideElements from './WidgetOverrideElements';

jest.mock('views/logic/withPluginEntities', () => (x) => x);

describe('WidgetOverrideElements', () => {
  it('renders original children if no elements are present', async () => {
    const { findByText } = render((
      <WidgetOverrideElements widgetOverrideElements={[]}>
        <span>Hello world!</span>
      </WidgetOverrideElements>
    ));

    await findByText('Hello world!');
  });

  it('renders original children if element does not throw', async () => {
    const { findByText } = render((
      <WidgetOverrideElements widgetOverrideElements={[() => null]}>
        <span>Hello world!</span>
      </WidgetOverrideElements>
    ));

    await findByText('Hello world!');
  });

  it('propagates thrown errors', async () => {
    suppressConsole(async () => {
      const throwElement = () => {
        throw new Error('The dungeon collapses, you die!');
      };

      expect(() => render((
        <WidgetOverrideElements widgetOverrideElements={[throwElement]}>
          <span>Hello world!</span>
        </WidgetOverrideElements>
      ))).toThrowError('The dungeon collapses, you die!');
    });
  });

  it('renders thrown component if element throws one', async () => {
    const Component = () => <span>I was thrown!</span>;

    const OverridingElement = ({ override }) => {
      useEffect(() => override(Component), []);

      return null;
    };

    const { findByText, queryByText } = render((
      <WidgetOverrideElements widgetOverrideElements={[OverridingElement]}>
        <span>Hello world!</span>
      </WidgetOverrideElements>
    ));

    await findByText('I was thrown!');

    expect(queryByText('Hello world!')).toBeNull();
  });
});
