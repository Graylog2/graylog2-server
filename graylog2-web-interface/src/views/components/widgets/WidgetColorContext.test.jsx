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
// @flow strict
import * as React from 'react';
import { mount } from 'wrappedEnzyme';

import type { ColorRule } from 'views/stores/ChartColorRulesStore';
import { ChartColorRulesActions } from 'views/stores/ChartColorRulesStore';

import WidgetColorContext from './WidgetColorContext';

import ChartColorContext from '../visualizations/ChartColorContext';
import type { ChangeColorFunction, ChartColorMap } from '../visualizations/ChartColorContext';

jest.mock('views/stores/ChartColorRulesStore', () => ({
  ChartColorRulesActions: {
    set: jest.fn(),
  },
}));

jest.mock('stores/connect', () => (x) => x);

type ContainerProps = {
  colors: ChartColorMap,
  setColor: ChangeColorFunction,
};

// eslint-disable-next-line no-unused-vars
const Container = ({ colors, setColor }: ContainerProps) => <div>Hello!</div>;

describe('WidgetColorContext', () => {
  const colorRules: Array<ColorRule> = [
    { widgetId: 'something', name: 'count()', color: '#414141' },
    { widgetId: 'else', name: 'sum(bytes)', color: '#123123' },
    { widgetId: 'deadbeef', name: 'sum(bytes)', color: '#affe42' },
    { widgetId: 'hello', name: 'TCP', color: '#FE2B39' },
    { widgetId: 'deadbeef', name: 'localhost', color: '#171EFE' },
  ];
  const wrapper = mount((
    <WidgetColorContext colorRules={colorRules} id="deadbeef">
      <ChartColorContext.Consumer>
        {({ colors, setColor }) => (
          <Container colors={colors} setColor={setColor} />
        )}
      </ChartColorContext.Consumer>
    </WidgetColorContext>
  ));
  const container = wrapper.find('Container');

  it('extracts coloring rules for current widget', () => {
    const { colors } = container.props();

    expect(colors).toEqual({ localhost: '#171EFE', 'sum(bytes)': '#affe42' });
    expect();
  });

  it('supplies setter for color of current widget', () => {
    const { setColor } = container.props();

    setColor('avg(took_ms)', '#FEFC67');

    expect(ChartColorRulesActions.set).toHaveBeenCalledWith('deadbeef', 'avg(took_ms)', '#FEFC67');
  });
});
