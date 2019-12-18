import React from 'react';
import { mount } from 'wrappedEnzyme';
import { List } from 'immutable';

import FieldType from 'views/logic/fieldtypes/FieldType';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import FieldList from './FieldList';

jest.mock('views/stores/ViewMetadataStore', () => ({
  ViewMetadataStore: {
    getInitialState: () => ({
      activeQuery: 'aQueryId',
      id: 'aViewId',
    }),
    listen: jest.fn(),
  },
}));

describe('<FieldList />', () => {
  const properties = [{ enumerable: true }];
  const fieldType1 = new FieldType('string', properties, []);
  const fieldTypeMapping1 = new FieldTypeMapping('date', fieldType1);

  const fieldType2 = new FieldType('string', properties, []);
  const fieldTypeMapping2 = new FieldTypeMapping('http_method', fieldType2);

  const allFields = new List([fieldTypeMapping1, fieldTypeMapping2]);
  const fields = new List([fieldTypeMapping2]);

  it('should render a FieldList', () => {
    const wrapper = mount(<FieldList allFields={allFields} fields={fields} maximumHeight={1000} />);
    expect(wrapper.find('span.field-element').text()).toBe('http_method');
  });

  it('should render all fields in FieldList after click', () => {
    const wrapper = mount(<FieldList allFields={allFields} fields={fields} maximumHeight={1000} />);
    expect(wrapper.find('span.field-element').length).toBe(1);

    wrapper.find('a[children="all"]').simulate('click');

    expect(wrapper.find('span.field-element').length).toBe(2);
    expect(wrapper.find('span.field-element').at(0).text()).toBe('date');
    expect(wrapper.find('span.field-element').at(1).text()).toBe('http_method');

    wrapper.find('a[children="current streams"]').simulate('click');
    expect(wrapper.find('span.field-element').length).toBe(1);
    expect(wrapper.find('span.field-element').text()).toBe('http_method');
  });

  it('should search in the field list', () => {
    const wrapper = mount(<FieldList allFields={allFields} fields={fields} maximumHeight={1000} />);
    expect(wrapper.find('span.field-element').length).toBe(1);

    wrapper.find('a[children="all"]').simulate('click');

    expect(wrapper.find('span.field-element').length).toBe(2);

    wrapper.find('input#common-search-form-query-input').simulate('change', { target: { value: 'http_method' } });

    expect(wrapper.find('span.field-element').length).toBe(1);
    expect(wrapper.find('span.field-element').text()).toBe('http_method');
  });

  it('should show hint when no fields are returned after filtering', () => {
    const hint = <i>No fields to show. Try changing your filter term or select a different field set above.</i>;
    const wrapper = mount(<FieldList allFields={allFields} fields={fields} maximumHeight={1000} />);
    expect(wrapper).not.toContainReact(hint);

    wrapper.find('input#common-search-form-query-input').simulate('change', { target: { value: 'non_existing_field' } });

    expect(wrapper.find('span.field-element').length).toBe(0);
    expect(wrapper).toContainReact(hint);
  });
});
