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

import { asMock } from 'helpers/mocking';
import type { ColorRule } from 'views/components/widgets/useColorRules';
import useColorRules from 'views/components/widgets/useColorRules';
import { setChartColor } from 'views/logic/slices/widgetActions';

import WidgetColorContext from './WidgetColorContext';

import ChartColorContext from '../visualizations/ChartColorContext';
import type { ChartColorContextType } from '../visualizations/ChartColorContext';

jest.mock('views/components/widgets/useColorRules');
jest.mock('views/stores/useViewsDispatch', () => () => jest.fn());

jest.mock('views/logic/slices/widgetActions', () => ({
  setChartColor: jest.fn(),
}));

const SUT = ({ fn }: { fn: (context: ChartColorContextType) => React.ReactNode }) => (
  <WidgetColorContext id="deadbeef">
    <ChartColorContext.Consumer>{fn}</ChartColorContext.Consumer>
  </WidgetColorContext>
);

describe('WidgetColorContext', () => {
  const colorRules: Array<ColorRule> = [
    { widgetId: 'something', name: 'count()', color: '#414141' },
    { widgetId: 'else', name: 'sum(bytes)', color: '#123123' },
    { widgetId: 'deadbeef', name: 'sum(bytes)', color: '#affe42' },
    { widgetId: 'hello', name: 'TCP', color: '#FE2B39' },
    { widgetId: 'deadbeef', name: 'localhost', color: '#171EFE' },
  ];

  beforeEach(() => {
    asMock(useColorRules).mockReturnValue(colorRules);
  });

  it('extracts coloring rules for current widget', async () => {
    const fn = ({ colors }: ChartColorContextType) => {
      expect(colors.get('localhost')).toEqual('#171EFE');
      expect(colors.get('sum(bytes)')).toEqual('#affe42');

      return <span>Done!</span>;
    };

    render(<SUT fn={fn} />);

    await screen.findByText('Done!');
  });

  it('supplies setter for color of current widget', async () => {
    const fn = ({ setColor }: ChartColorContextType) => {
      setColor('avg(took_ms)', '#FEFC67');

      return <span>Done!</span>;
    };

    render(<SUT fn={fn} />);

    await screen.findByText('Done!');

    expect(setChartColor).toHaveBeenCalledWith('deadbeef', 'avg(took_ms)', '#FEFC67');
  });
});
