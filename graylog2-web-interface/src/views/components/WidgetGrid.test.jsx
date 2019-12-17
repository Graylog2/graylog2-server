import React from 'react';
import renderer from 'react-test-renderer';
import Immutable from 'immutable';
import { mount } from 'wrappedEnzyme';

import WidgetPosition from 'views/logic/widgets/WidgetPosition';
import Widget from 'views/components/widgets/Widget';
import _Widget from 'views/logic/widgets/Widget';
import WidgetGrid from './WidgetGrid';

jest.mock('./widgets/Widget', () => () => 'widget');
// eslint-disable-next-line react/prop-types
jest.mock('components/common/ReactGridContainer', () => ({ children }) => <react-grid-container-mock>{children}</react-grid-container-mock>);
jest.mock('graylog-web-plugin/plugin', () => ({
  PluginStore: {
    exports: key => (key !== 'enterpriseWidgets' ? [] : [
      {
        type: 'dummy',
        displayName: 'Some Dummy Visualization',
        defaultHeight: 5,
        defaultWidth: 6,
      },
    ]),
  },
}));

jest.mock('react-sizeme', () => ({
  // eslint-disable-next-line react/prop-types
  SizeMe: ({ children }) => <div>{children({ size: { width: 200 } })}</div>,
}));

describe('<WidgetGrid />', () => {
  it('should render with minimal props', () => {
    const wrapper = renderer.create((
      <WidgetGrid allFields={Immutable.List()}
                  data={{}}
                  errors={{}}
                  onPositionsChange={() => {}}
                  titles={Immutable.Map()}
                  widgets={{}}
                  fields={Immutable.List()} />
    ));
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
    const wrapper = mount((
      <WidgetGrid widgets={widgets}
                  errors={{}}
                  positions={positions}
                  data={data}
                  titles={titles}
                  allFields={Immutable.List()}
                  fields={Immutable.List()}
                  onPositionsChange={() => {}} />
    ));
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
    const wrapper = mount((
      <WidgetGrid widgets={widgets}
                  errors={{}}
                  positions={positions}
                  data={data}
                  titles={titles}
                  allFields={Immutable.List()}
                  fields={Immutable.List()}
                  onPositionsChange={() => {}} />
    ));
    expect(wrapper.find(Widget)).toHaveLength(1);
  });
});
