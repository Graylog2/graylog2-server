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
import { asMock } from 'helpers/mocking';
import usePluginEntities from 'hooks/usePluginEntities';

import type { OverrideProps } from './WidgetOverrideElements';
import WidgetOverrideElements from './WidgetOverrideElements';

jest.mock('hooks/usePluginEntities');

describe('WidgetOverrideElements', () => {
  it('renders original children if no elements are present', async () => {
    asMock(usePluginEntities).mockReturnValue([]);
    const { findByText } = render((
      <WidgetOverrideElements>
        <span>Hello world!</span>
      </WidgetOverrideElements>
    ));

    await findByText('Hello world!');
  });

  it('renders original children if element does not throw', async () => {
    asMock(usePluginEntities).mockReturnValue([() => null]);
    const { findByText } = render((
      <WidgetOverrideElements>
        <span>Hello world!</span>
      </WidgetOverrideElements>
    ));

    await findByText('Hello world!');
  });

  it('propagates thrown errors', async () => {
    await suppressConsole(() => {
      const throwElement = () => {
        throw new Error('The dungeon collapses, you die!');
      };

      asMock(usePluginEntities).mockReturnValue([throwElement]);

      expect(() => render((
        <WidgetOverrideElements>
          <span>Hello world!</span>
        </WidgetOverrideElements>
      ))).toThrow('The dungeon collapses, you die!');
    });
  });

  it('renders thrown component if element throws one', async () => {
    const Component = () => <div>I was thrown!</div>;

    const OverridingElement = ({ override }: OverrideProps) => {
      useEffect(() => override(Component), [override]);

      return null;
    };

    asMock(usePluginEntities).mockReturnValue([OverridingElement]);

    const { findByText, queryByText } = render((
      <WidgetOverrideElements>
        <span>Hello world!</span>
      </WidgetOverrideElements>
    ));

    await findByText('I was thrown!');

    expect(queryByText('Hello world!')).toBeNull();
  });
});
