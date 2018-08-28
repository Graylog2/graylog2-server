import React from 'react';
import renderer from 'react-test-renderer';
import Immutable from 'immutable';
import { mount } from 'enzyme';

import mockComponent from 'helpers/mocking/MockComponent';
import WidgetPosition from 'enterprise/logic/widgets/WidgetPosition';
import Widget from 'enterprise/components/widgets/Widget';
import _Widget from 'enterprise/logic/widgets/Widget';
import WidgetGrid from './WidgetGrid';

jest.mock('./widgets/Widget', () => mockComponent('Widget'));
jest.mock('components/common/ReactGridContainer', () => mockComponent('ReactGridContainer'));
jest.mock('graylog-web-plugin/plugin', () => ({
  PluginStore: {
    exports: () => ([
      {
        type: 'dummy',
        displayName: 'Some Dummy Visualization',
        defaultHeight: 5,
        defaultWidth: 6,
      },
    ]),
  },
}));

describe('<WidgetGrid />', () => {
  it('should render with empty props', () => {
    const wrapper = renderer.create(<WidgetGrid />);
    expect(wrapper.toJSON()).toMatchSnapshot();
  });

  it('should render with widgets passed', () => {
    const widgets = {
      widget1: _Widget.builder().type('dummy').id('widget1').build(),
    };
    const positions = {
      widget1: new WidgetPosition(1, 1, 1, 1),
    };
    const data = {
      widget1: [],
    };

    const titles = Immutable.Map({
      widget1: 'A dummy widget',
    });
    const wrapper = mount(<WidgetGrid widgets={widgets} positions={positions} data={data} titles={titles} />);
    expect(wrapper.find(Widget)).toHaveLength(1);
  });

  it('should render widget even if widget has no data', () => {
    const widgets = {
      widget1: _Widget.builder().type('dummy').id('widget1').build(),
    };
    const positions = {
      widget1: new WidgetPosition(1, 1, 1, 1),
    };
    const data = {
    };

    const titles = Immutable.Map({
      widget1: 'A dummy widget',
    });
    const wrapper = mount(<WidgetGrid widgets={widgets} positions={positions} data={data} titles={titles} />);
    expect(wrapper.find(Widget)).toHaveLength(1);
  });
});