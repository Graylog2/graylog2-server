// @flow strict
import * as React from 'react';
import { mount } from 'theme/enzymeWithTheme';
import { AdditionalContext } from 'views/logic/ActionContext';

import SelectExtractorType from './SelectExtractorType';
import FieldType from '../fieldtypes/FieldType';

jest.mock('logic/datetimes/DateTime', () => ({}));

describe('SelectExtractorType', () => {
  const value = 'value of message';
  const field = 'value_field';
  const focus = jest.fn();
  window.open = jest.fn(() => { return { focus }; });

  const message = {
    fields: {
      gl2_source_input: 'input-id',
      gl2_source_node: 'node-id',
    },
    formattedFields: {},
    id: 'message-id',
    index: 'message-index',
  };

  it('should render', () => {
    const wrapper = mount(
      <AdditionalContext.Provider key="message-key" value={{ message }}>
        <SelectExtractorType onClose={() => {}} value={value} field={field} queryId="foo" type={FieldType.Unknown} />
      </AdditionalContext.Provider>,
    );
    const bootstrapModalForm = wrapper.find('BootstrapModalForm');
    expect(bootstrapModalForm).toExist();
    expect(bootstrapModalForm).toHaveProp('show', true);
  });

  it('should select a extractor and open a new window', () => {
    const wrapper = mount(
      <AdditionalContext.Provider key="message-key" value={{ message }}>
        <SelectExtractorType onClose={() => {}} value={value} field={field} queryId="foo" type={FieldType.Unknown} />
      </AdditionalContext.Provider>,
    );
    const select = wrapper.find('Select');
    const { onChange } = select.at(0).props();

    const form = wrapper.find('form');
    expect(select).toExist();
    expect(select.at(0)).toHaveProp('placeholder', 'Select extractor type');

    onChange('grok');
    form.simulate('submit');
    expect(window.open).toHaveBeenCalled();
    expect(focus).toHaveBeenCalled();
  });
});
