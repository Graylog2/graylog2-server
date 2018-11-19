import React from 'react';
import { mount } from 'enzyme';

import mockComponent from 'helpers/mocking/MockComponent';
import FieldType from 'enterprise/logic/fieldtypes/FieldType';
import UserTimezoneTimestamp from 'enterprise/components/common/UserTimezoneTimestamp';

import Value from './Value';

jest.mock('./ValueActions', () => mockComponent('ValueActions'));
jest.mock('enterprise/components/common/UserTimezoneTimestamp', () => mockComponent('UserTimezoneTimestamp'));

describe('Value', () => {
  it('render without type information but no children', () => {
    const wrapper = mount(<Value field="foo" queryId="someQueryId" value={42} />);
    const valueActions = wrapper.find('ValueActions');
    expect(valueActions).toIncludeText('foo = 42');
  });
  it('renders timestamps with a custom component', () => {
    const wrapper = mount(<Value field="foo" queryId="someQueryId" value="2018-10-02T14:45:40Z" type={new FieldType('date', [], [])} />);
    const valueActions = wrapper.find('ValueActions');
    expect(valueActions).toContainReact(<UserTimezoneTimestamp dateTime="2018-10-02T14:45:40Z" />);
  });
  it('renders booleans as strings', () => {
    const wrapper = mount(<Value field="foo" queryId="someQueryId" value={false} type={new FieldType('boolean', [], [])} />);
    const valueActions = wrapper.find('ValueActions');
    expect(valueActions).toIncludeText('foo = false');
  });
  it('truncates values longer than 30 characters', () => {
    const wrapper = mount(<Value field="message"
                                 queryId="someQueryId"
                                 value="sophon unbound: [84785:0] error: outgoing tcp: connect: Address already in use for 1.0.0.1"
                                 type={new FieldType('string', [], [])} />);
    const valueActions = wrapper.find('ValueActions');
    expect(valueActions).toIncludeText('message = sophon unbound: [84785:0] e...');
  });
});