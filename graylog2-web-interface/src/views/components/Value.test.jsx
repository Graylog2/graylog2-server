/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import React from 'react';
import { mount } from 'wrappedEnzyme';
import each from 'jest-each';
import mockComponent from 'helpers/mocking/MockComponent';

import FieldType from 'views/logic/fieldtypes/FieldType';

import Value from './Value';
import EmptyValue from './EmptyValue';
import InteractiveContext from './contexts/InteractiveContext';

jest.mock('./actions/ValueActions', () => mockComponent('ValueActions'));
jest.mock('views/components/common/UserTimezoneTimestamp', () => mockComponent('UserTimezoneTimestamp'));

describe('Value', () => {
  describe('shows value actions menu', () => {
    const Component = (props) => <Value {...props} />;

    it('render without type information but no children', () => {
      const wrapper = mount(<Component field="foo" queryId="someQueryId" value={42} />);
      const valueActions = wrapper.find('ValueActions');

      expect(valueActions).toIncludeText('foo = 42');
    });

    it('renders timestamps with a custom component', () => {
      const wrapper = mount(<Component field="foo"
                                       queryId="someQueryId"
                                       value="2018-10-02T14:45:40Z"
                                       type={new FieldType('date', [], [])} />);
      const userTimestamp = wrapper.find('UserTimezoneTimestamp');

      expect(userTimestamp).toExist();
    });

    it('renders numeric timestamps with a custom component', () => {
      const wrapper = mount(<Component field="foo"
                                       queryId="someQueryId"
                                       value={1571302317}
                                       type={new FieldType('date', [], [])} />);
      const userTimeStamp = wrapper.find('UserTimezoneTimestamp');

      expect(userTimeStamp).toExist();
    });

    it('renders booleans as strings', () => {
      const wrapper = mount(<Component field="foo"
                                       queryId="someQueryId"
                                       value={false}
                                       type={new FieldType('boolean', [], [])} />);
      const valueActions = wrapper.find('ValueActions');

      expect(valueActions).toIncludeText('foo = false');
    });

    it('renders booleans as strings even if field type is unknown', () => {
      const wrapper = mount(<Component field="foo" queryId="someQueryId" value={false} type={FieldType.Unknown} />);
      const valueActions = wrapper.find('ValueActions');

      expect(valueActions).toIncludeText('foo = false');
    });

    it('renders arrays as strings', () => {
      const wrapper = mount(<Component field="foo"
                                       queryId="someQueryId"
                                       value={[23, 'foo']}
                                       type={FieldType.Unknown} />);
      const valueActions = wrapper.find('ValueActions');

      expect(valueActions).toIncludeText('foo = [23,"foo"]');
    });

    it('renders objects as strings', () => {
      const wrapper = mount(<Component field="foo"
                                       queryId="someQueryId"
                                       value={{ foo: 23 }}
                                       type={FieldType.Unknown} />);
      const valueActions = wrapper.find('ValueActions');

      expect(valueActions).toIncludeText('foo = {"foo":23}');
    });

    it('truncates values longer than 30 characters', () => {
      const wrapper = mount(<Component field="message"
                                       queryId="someQueryId"
                                       value="sophon unbound: [84785:0] error: outgoing tcp: connect: Address already in use for 1.0.0.1"
                                       type={new FieldType('string', [], [])} />);
      const valueActions = wrapper.find('ValueActions');

      expect(valueActions).toIncludeText('message = sophon unbound: [84785:0] e...');
    });
  });

  each([true, false]).describe('setting interactive context to `%p`', (interactive) => {
    const Component = (props) => (
      <InteractiveContext.Provider value={interactive}>
        <Value {...props} />
      </InteractiveContext.Provider>
    );

    it('renders without type information but no children', () => {
      const wrapper = mount(<Component field="foo" queryId="someQueryId" value={42} />);
      const typeSpecificValue = wrapper.find('TypeSpecificValue');

      expect(typeSpecificValue).toHaveValue(42);
    });

    it('renders timestamps with a custom component', () => {
      const wrapper = mount(<Component field="foo"
                                       queryId="someQueryId"
                                       value="2018-10-02T14:45:40Z"
                                       type={new FieldType('date', [], [])} />);
      const typeSpecificValue = wrapper.find('TypeSpecificValue');

      expect(typeSpecificValue).toHaveValue('2018-10-02T14:45:40Z');
    });

    it('renders booleans', () => {
      const wrapper = mount(<Component field="foo"
                                       queryId="someQueryId"
                                       value={false}
                                       type={new FieldType('boolean', [], [])} />);
      const typeSpecificValue = wrapper.find('TypeSpecificValue');

      expect(typeSpecificValue).toHaveValue(false);
    });

    it('renders arrays', () => {
      const wrapper = mount(<Component field="foo"
                                       queryId="someQueryId"
                                       value={[23, 'foo']}
                                       type={FieldType.Unknown} />);
      const typeSpecificValue = wrapper.find('TypeSpecificValue');

      expect(typeSpecificValue).toHaveProp('value', [23, 'foo']);
    });

    it('renders objects', () => {
      const wrapper = mount(<Component field="foo"
                                       queryId="someQueryId"
                                       value={{ foo: 23 }}
                                       type={FieldType.Unknown} />);
      const typeSpecificValue = wrapper.find('TypeSpecificValue');

      expect(typeSpecificValue).toHaveProp('value', { foo: 23 });
    });
  });

  describe('handles value action menu depending on interactive context', () => {
    const component = (interactive) => (props) => (
      <InteractiveContext.Provider value={interactive}>
        <Value {...props} />
      </InteractiveContext.Provider>
    );

    it('does not show value actions if interactive context is `false`', () => {
      const NoninteractiveComponent = component(false);
      const wrapper = mount(<NoninteractiveComponent field="foo"
                                                     queryId="someQueryId"
                                                     value={{ foo: 23 }}
                                                     type={FieldType.Unknown} />);
      const valueActions = wrapper.find('ValueActions');

      expect(valueActions).not.toExist();

      const typeSpecificValue = wrapper.find('TypeSpecificValue');

      expect(typeSpecificValue).toHaveProp('value', { foo: 23 });
    });

    it('shows value actions if interactive context is `true`', () => {
      const InteractiveComponent = component(true);
      const wrapper = mount(<InteractiveComponent field="foo"
                                                  queryId="someQueryId"
                                                  value={{ foo: 23 }}
                                                  type={FieldType.Unknown} />);
      const valueActions = wrapper.find('ValueActions');

      expect(valueActions).toExist();

      const typeSpecificValue = wrapper.find('TypeSpecificValue');

      expect(typeSpecificValue).toHaveProp('value', { foo: 23 });
    });
  });

  const verifyReplacementOfEmptyValues = ({ value }) => {
    const wrapper = mount(<Value field="foo" queryId="someQueryId" value={value} />);
    const valueActions = wrapper.find('ValueActions');

    expect(valueActions).toIncludeText('foo = <Empty Value>');
    expect(valueActions).toContainReact(<EmptyValue />);
  };

  it.each`
    value
    ${'\u205f'}
    ${''}
    ${' '}
  `('renders (unicode) spaces as `EmptyValue` component', verifyReplacementOfEmptyValues);
});
