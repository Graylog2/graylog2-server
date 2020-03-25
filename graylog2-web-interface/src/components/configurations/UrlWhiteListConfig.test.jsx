// @flow strict
import React from 'react';
import { mount } from 'wrappedEnzyme';

import 'helpers/mocking/react-dom_mock';
import UrlWhiteListConfig from './UrlWhiteListConfig';

describe('UrlWhiteListConfig', () => {
  describe('render the UrlWhiteListConfig component', () => {
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
    it('should create new instance', () => {
      const wrapper = mount(<UrlWhiteListConfig config={config}
                                                updateConfig={onUpdate} />);
      expect(wrapper).toMatchSnapshot();
    });
    it('should display Url list table', () => {
      const wrapper = mount(<UrlWhiteListConfig config={config}
                                                updateConfig={onUpdate} />);
      expect(wrapper.find('.table-bordered')).toBeDefined();
      expect(wrapper.find('tbody>tr')).toHaveLength(config.entries.length);
    });
  });
});
