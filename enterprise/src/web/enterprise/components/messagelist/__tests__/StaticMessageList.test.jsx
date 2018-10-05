import React from 'react';
import { mount } from 'enzyme';
import mockComponent from 'helpers/mocking/MockComponent';

import MessageList from 'enterprise/components/widgets/MessageList';
import EmptyResultWidget from 'enterprise/components/widgets/EmptyResultWidget';

import StaticMessageList from '../StaticMessageList';
import * as fixtures from './StaticMessageList.fixtures';

jest.mock('stores/connect', () => x => x);
jest.mock('enterprise/stores/FieldTypesStore', () => ({ FieldTypesStore: {} }));
jest.mock('enterprise/components/widgets/MessageList', () => mockComponent('MessageList'));

describe('StaticMessageList', () => {
  it('renders without messages', () => {
    const wrapper = mount(<StaticMessageList fieldTypes={{}}
                                             onToggleMessages={() => {}}
                                             showMessages />);
    expect(wrapper.find(EmptyResultWidget)).toHaveLength(1);
  });
  it('renders when empty messages are passed', () => {
    const wrapper = mount(<StaticMessageList fieldTypes={{}}
                                             onToggleMessages={() => {}}
                                             showMessages
                                             messages={{ total: 0, messages: [] }} />);
    expect(wrapper.find(EmptyResultWidget)).toHaveLength(1);
  });
  it('renders messages list', () => {
    const wrapper = mount(<StaticMessageList fieldTypes={{}}
                                             onToggleMessages={() => {}}
                                             showMessages
                                             messages={fixtures.tenMessages} />);
    const messageList = wrapper.find(MessageList);
    expect(messageList).toHaveLength(1);
    expect(messageList).toHaveProp('data', fixtures.tenMessages);
  });
});
