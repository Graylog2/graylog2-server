import React from 'react';
import { mount } from 'enzyme';
import { PluginStore } from 'graylog-web-plugin/plugin';

import AddWidgetButton from './AddWidgetButton';

const mockAggregateActionHandler = jest.fn();
const mockAddMessageCountActionHandler = jest.fn();
const mockAddMessageTableActionHandler = jest.fn();

const MockCreateParameterDialog = () => {
  return <span>42</span>;
};

const bindings = {
  creators: [
    {
      type: 'preset',
      title: 'Message Count',
      func: mockAddMessageCountActionHandler,
    },
    {
      type: 'preset',
      title: 'Message Table',
      func: mockAddMessageTableActionHandler,
    },
    {
      type: 'generic',
      title: 'Custom Aggregation',
      func: mockAggregateActionHandler,
    },
    {
      type: 'generic',
      title: 'Parameter',
      component: MockCreateParameterDialog,
    },
  ],

};

const plugin = {
  exports: bindings,
  metadata: {
    name: 'Dummy Plugin for Tests',
  },
};

describe('AddWidgetButton', () => {
  beforeEach(() => {
    PluginStore.register(plugin);
  });
  afterEach(() => {
    PluginStore.unregister(plugin);
  });

  const onClick = jest.fn();

  it('renders a dropdown', () => {
    const wrapper = mount(<AddWidgetButton onClick={onClick} toggleAutoClose={onClick} />);

    const buttonGroup = wrapper.find('ButtonGroup').at(0);
    expect(buttonGroup).toHaveLength(1);
    expect(buttonGroup.find('button')).toHaveLength(4);
  });
  it('contains menu items for all widget types', () => {
    const wrapper = mount(<AddWidgetButton onClick={onClick} toggleAutoClose={onClick} />);

    const dropdownButton = wrapper.find('ButtonGroup');
    ['Custom Aggregation', 'Message Count', 'Message Table', 'Parameter']
      .forEach(title => expect(dropdownButton.find(`button[children="${title}"]`)).toExist());
  });
  it('clicking on option to add aggregation calls AggregateActionHandler', () => {
    const wrapper = mount(<AddWidgetButton onClick={onClick} toggleAutoClose={onClick} />);

    const addAggregation = wrapper.find('button[children="Custom Aggregation"]');
    expect(addAggregation).toExist(0);
    addAggregation.simulate('click');

    expect(mockAggregateActionHandler).toHaveBeenCalled();
  });
  it('clicking on option to add message count calls AddMessageCountActionHandler', () => {
    const wrapper = mount(<AddWidgetButton onClick={onClick} toggleAutoClose={onClick} />);

    const addMessageCount = wrapper.find('button[children="Message Count"]');
    expect(addMessageCount).toExist(0);
    addMessageCount.simulate('click');

    expect(mockAddMessageCountActionHandler).toHaveBeenCalled();
  });
  it('clicking on option to add message table calls AddMessageTableActionHandler', () => {
    const wrapper = mount(<AddWidgetButton onClick={onClick} toggleAutoClose={onClick} />);

    const addMessageTable = wrapper.find('button[children="Message Table"]');
    expect(addMessageTable).toExist(0);
    addMessageTable.simulate('click');

    expect(mockAddMessageTableActionHandler).toHaveBeenCalled();
  });
  it('clicking on option to add a parameter renders MockCreateParameterDialog', () => {
    const wrapper = mount(<AddWidgetButton onClick={onClick} toggleAutoClose={onClick} />);

    const addMessageTable = wrapper.find('button[children="Parameter"]');
    expect(addMessageTable).toExist(0);
    addMessageTable.simulate('click');

    wrapper.update();

    expect(wrapper.find('MockCreateParameterDialog')).toExist();
  });
  it('calling onClose from creator component removes it', () => {
    const wrapper = mount(<AddWidgetButton onClick={onClick} toggleAutoClose={onClick} />);

    const addMessageTable = wrapper.find('button[children="Parameter"]');
    expect(addMessageTable).toExist(0);
    addMessageTable.simulate('click');

    wrapper.update();

    const mockCreateParameterDialog = wrapper.find('MockCreateParameterDialog');

    expect(mockCreateParameterDialog).toExist();

    const { onClose } = mockCreateParameterDialog.props();
    onClose();

    wrapper.update();
    expect(wrapper.find('MockCreateParameterDialog')).not.toExist();
  });
});
