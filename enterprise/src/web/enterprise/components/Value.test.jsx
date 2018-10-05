import React from 'react';
import { mount } from 'enzyme';

import mockComponent from 'helpers/mocking/MockComponent';
import Value from './Value';
import FieldType from '../logic/fieldtypes/FieldType';

jest.mock('./OverlayDropdown', () => mockComponent('OverlayDropdown'));
jest.mock('enterprise/components/common/UserTimezoneTimestamp', () => mockComponent('UserTimezoneTimestamp'));

describe('Value', () => {
  it('render without type information but no children', () => {
    const wrapper = mount(<Value field="foo" queryId="someQueryId" value={42} />);
    expect(wrapper).toMatchSnapshot();
  });
  it('renders timestamps with a custom component', () => {
    const wrapper = mount(<Value field="foo" queryId="someQueryId" value="2018-10-02T14:45:40Z" type={new FieldType('date', [], [])} />);
    expect(wrapper).toMatchSnapshot();
  });
  it('renders booleans as strings', () => {
    const wrapper = mount(<Value field="foo" queryId="someQueryId" value={false} type={new FieldType('boolean', [], [])} />);
    expect(wrapper).toMatchSnapshot();
  });
});