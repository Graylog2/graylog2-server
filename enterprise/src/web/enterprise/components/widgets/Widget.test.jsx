import React from 'react';
import renderer from 'react-test-renderer';
import { mount } from 'enzyme';

import mockComponent from 'helpers/mocking/MockComponent';
import Widget from './Widget';
import WidgetPosition from '../../logic/widgets/WidgetPosition';
import LoadingWidget from './LoadingWidget';

jest.mock('../searchbar/QueryInput', () => mockComponent('QueryInput'));
jest.mock('./WidgetHeader', () => mockComponent('WidgetHeader'));
jest.mock('graylog-web-plugin/plugin', () => ({
  PluginStore: {
    exports: () => ([
      {
        type: 'dummy',
        displayName: 'Some Dummy Visualization',
        visualizationComponent: mockComponent('DummyVisualization'),
      },
    ]),
  },
}));

const createNodeMock = (element) => {
  if (element.type === 'div') {
    return {
      clientHeight: 420,
      clientWidth: 420,
    };
  }
  return null;
};

describe('<Widget />', () => {
  it('should render with empty props', () => {
    const options = { createNodeMock };
    const wrapper = renderer.create((
      <Widget widget={{}}
              onSizeChange={() => {}}
              position={new WidgetPosition(1, 1, 1, 1)} />
    ), options);
    expect(wrapper.toJSON()).toMatchSnapshot();
  });
  it('should render loading widget for widget without data', () => {
    const wrapper = mount((
      <Widget widget={{}}
              onSizeChange={() => {}}
              position={new WidgetPosition(1, 1, 1, 1)} />
    ));
    expect(wrapper.find(LoadingWidget)).toHaveLength(1);
  });
  it('should render correct widget visualization for widget with data', () => {
    const wrapper = mount((
      <Widget widget={{
        type: 'dummy',
      }}
              onSizeChange={() => {}}
              data={[]}
              position={new WidgetPosition(1, 1, 1, 1)} />
    ));
    expect(wrapper.find(LoadingWidget)).toHaveLength(0);
    expect(wrapper.find('DummyVisualization')).toHaveLength(1);
  });
});
