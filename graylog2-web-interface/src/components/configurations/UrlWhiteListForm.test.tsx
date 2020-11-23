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
import React from 'react';
import { mount } from 'wrappedEnzyme';

import 'helpers/mocking/react-dom_mock';
import UrlWhiteListForm from './UrlWhiteListForm';

describe('UrlWhitelistForm', () => {
  let wrapper;
  const setState = jest.fn();
  const useStateSpy: jest.SpyInstance<[any, React.Dispatch<any>]> = jest.spyOn(React, 'useState');

  useStateSpy.mockImplementation((init) => [init, setState]);
  const onUpdate = jest.fn();
  const config = {
    entries: [
      {
        id: 'f7033f1f-d50f-4323-96df-294ede41d951',
        value: 'http://localhost:8080/system/',
        title: 'testam',
        type: 'regex',
      },
      {
        id: '636a2d40-c4c5-40b9-ab3a-48cf7978e9af',
        value: 'http://localhost:8080/system/',
        title: 'test',
        type: 'regex',
      },
      {
        id: 'f28fd891-5f2d-4128-9a94-e97c1ab07a1f',
        value: 'http://localhost:8080/system/',
        title: 'test',
        type: 'literal',
      },
    ],
    disabled: false,
  };

  afterEach(() => {
    jest.clearAllMocks();
  });

  describe('render the UrlWhitelistForm component', () => {
    it('should create new instance', () => {
      wrapper = mount(<UrlWhiteListForm urls={config.entries}
                                        disabled={config.disabled}
                                        onUpdate={() => {}} />);

      expect(wrapper).toExist();
    });

    it('should display Url form list table', () => {
      wrapper = mount(<UrlWhiteListForm urls={config.entries}
                                        disabled={config.disabled}
                                        onUpdate={onUpdate} />);

      expect(wrapper.find('.form-group')).toBeDefined();

      const title = wrapper.find('input#title-input0').at(0);

      expect(title.instance()?.value).toBe(config.entries[0].title);
    });

    it('should validate and update on input change', () => {
      wrapper = mount(<UrlWhiteListForm urls={config.entries}
                                        disabled={config.disabled}
                                        onUpdate={onUpdate} />);

      const title = wrapper.find('input#title-input0').at(0);
      const instance = title.instance();

      if (instance) {
        instance.value = 'world';
      }

      title.simulate('change');

      expect(onUpdate).toHaveBeenCalled();
    });

    it('should add a new row to the form', () => {
      wrapper = mount(<UrlWhiteListForm urls={config.entries}
                                        disabled={config.disabled}
                                        onUpdate={onUpdate} />);

      const button = wrapper.find('button').at(0);

      button.simulate('click');
      const listItems = wrapper.find('tbody>tr');

      expect(listItems.length).toBe(config.entries.length + 1);
    });

    it('should delete a row', () => {
      wrapper = mount(<UrlWhiteListForm urls={config.entries}
                                        disabled={config.disabled}
                                        onUpdate={onUpdate} />);

      const deleteButton = wrapper.find('svg.fa-trash-alt').at(0);

      deleteButton.simulate('click');
      const listItems = wrapper.find('tbody>tr');

      expect(listItems.length).toBe(config.entries.length - 1);
    });
  });
});
