import React from 'react';
import { mount } from 'enzyme';

import AddWidgetButton from './AddWidgetButton';

let mockAggregateActionHandler;
jest.mock('enterprise/logic/fieldactions/AggregateActionHandler', () => { mockAggregateActionHandler = jest.fn(); return mockAggregateActionHandler; });

let mockAddMessageCountActionHandler;
jest.mock('enterprise/logic/fieldactions/AddMessageCountActionHandler', () => { mockAddMessageCountActionHandler = jest.fn(); return mockAddMessageCountActionHandler; });

let mockAddMessageTableActionHandler;
jest.mock('enterprise/logic/fieldactions/AddMessageTableActionHandler', () => { mockAddMessageTableActionHandler = jest.fn(); return mockAddMessageTableActionHandler; });

describe('AddWidgetButton', () => {
  it('renders a dropdown', () => {
    const wrapper = mount(<AddWidgetButton />);

    const dropdownButton = wrapper.find('DropdownButton');
    expect(dropdownButton).toHaveLength(1);
    expect(dropdownButton.find('MenuItem')).toHaveLength(4);
  });
  it('contains menu items for all widget types', () => {
    const wrapper = mount(<AddWidgetButton />);

    const dropdownButton = wrapper.find('DropdownButton');
    ['Custom Aggregation', 'Message Count', 'Message Table']
      .forEach(title => expect(dropdownButton.find(`a[children="${title}"]`)).toExist());
  });
  it('clicking on option to add aggregation calls AggregateActionHandler', () => {
    const wrapper = mount(<AddWidgetButton />);

    const addAggregation = wrapper.find('a[children="Custom Aggregation"]');
    expect(addAggregation).toExist(0);
    addAggregation.simulate('click');

    expect(mockAggregateActionHandler).toHaveBeenCalled();
    expect(mockAggregateActionHandler).toHaveBeenCalledWith('', 'timestamp');
  });
  it('clicking on option to add message count calls AddMessageCountActionHandler', () => {
    const wrapper = mount(<AddWidgetButton />);

    const addMessageCount = wrapper.find('a[children="Message Count"]');
    expect(addMessageCount).toExist(0);
    addMessageCount.simulate('click');

    expect(mockAddMessageCountActionHandler).toHaveBeenCalled();
  });
  it('clicking on option to add message table calls AddMessageTableActionHandler', () => {
    const wrapper = mount(<AddWidgetButton />);

    const addMessageTable = wrapper.find('a[children="Message Table"]');
    expect(addMessageTable).toExist(0);
    addMessageTable.simulate('click');

    expect(mockAddMessageTableActionHandler).toHaveBeenCalled();
  });
});