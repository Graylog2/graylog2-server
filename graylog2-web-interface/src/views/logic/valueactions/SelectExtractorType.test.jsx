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
// @flow strict
import * as React from 'react';
import { mount } from 'wrappedEnzyme';

import { AdditionalContext } from 'views/logic/ActionContext';

import SelectExtractorType from './SelectExtractorType';

import FieldType from '../fieldtypes/FieldType';

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
