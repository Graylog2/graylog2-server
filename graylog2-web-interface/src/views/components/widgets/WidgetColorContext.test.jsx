// @flow strict
import * as React from 'react';
import { mount } from 'wrappedEnzyme';

import type { ColorRule } from 'views/stores/ChartColorRulesStore';
import { ChartColorRulesActions } from 'views/stores/ChartColorRulesStore';
import ChartColorContext from '../visualizations/ChartColorContext';
import type { ChangeColorFunction, RetrieveColorFunction } from '../visualizations/ChartColorContext';

import WidgetColorContext from './WidgetColorContext';

jest.mock('views/stores/ChartColorRulesStore', () => ({
  ChartColorRulesActions: {
    set: jest.fn(),
  },
}));

jest.mock('stores/connect', () => (x) => x);

type ContainerProps = {
  getColor: RetrieveColorFunction,
  setColor: ChangeColorFunction,
};

// eslint-disable-next-line no-unused-vars
const Container = ({ getColor, setColor }: ContainerProps) => <div>Hello!</div>;

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
        {({ getColor, setColor }) => (
          <Container getColor={getColor} setColor={setColor} />
        )}
      </ChartColorContext.Consumer>
    </WidgetColorContext>
  ));
  const container = wrapper.find('Container');

  it('extracts coloring rules for current widget', () => {
    const { getColor } = container.props();
    expect(getColor('localhost')).toEqual('#171EFE');
    expect(getColor('sum(bytes)')).toEqual('#affe42');
  });
  it('supplies setter for color of current widget', () => {
    const { setColor } = container.props();
    setColor('avg(took_ms)', '#FEFC67');

    expect(ChartColorRulesActions.set).toHaveBeenCalledWith('deadbeef', 'avg(took_ms)', '#FEFC67');
  });
});
