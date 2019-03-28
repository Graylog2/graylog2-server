// @flow strict
import * as React from 'react';
import * as Immutable from 'immutable';
import { mount } from 'enzyme';

// $FlowFixMe: imports from core need to be fixed in flow
import mockComponent from 'helpers/mocking/MockComponent';

import MessageList from 'enterprise/components/widgets/MessageList';
import EmptyResultWidget from 'enterprise/components/widgets/EmptyResultWidget';

import StaticMessageList from '../StaticMessageList';
import * as fixtures from './StaticMessageList.fixtures';

jest.mock('stores/connect', () => x => x);
jest.mock('enterprise/stores/FieldTypesStore', () => ({ FieldTypesStore: {} }));
jest.mock('enterprise/components/widgets/MessageList', () => mockComponent('MessageList'));

const selectedFields = Immutable.Set(['source', 'messages']);

describe('StaticMessageList', () => {
  it('renders without messages', () => {
    const wrapper = mount(<StaticMessageList fieldTypes={{}}
                                             onToggleMessages={() => {}}
                                             selectedFields={selectedFields}
                                             showMessages />);
    expect(wrapper.find(EmptyResultWidget)).toHaveLength(1);
  });
  it('renders when empty messages are passed', () => {
    const wrapper = mount(<StaticMessageList fieldTypes={{}}
                                             onToggleMessages={() => {}}
                                             selectedFields={selectedFields}
                                             showMessages
                                             messages={{ total: 0, messages: [] }} />);
    expect(wrapper.find(EmptyResultWidget)).toHaveLength(1);
  });
  it('renders messages list', () => {
    const wrapper = mount(<StaticMessageList fieldTypes={{}}
                                             onToggleMessages={() => {}}
                                             selectedFields={selectedFields}
                                             showMessages
                                             messages={fixtures.tenMessages} />);
    const messageList = wrapper.find(MessageList);
    expect(messageList).toHaveLength(1);
    expect(messageList).toHaveProp('data', fixtures.tenMessages);
  });
});
