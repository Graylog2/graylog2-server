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
import mockComponent from 'helpers/mocking/MockComponent';

import DataAdapterCreate from './DataAdapterCreate';

jest.mock('components/common', () => ({
  Select: mockComponent('SelectMock'),
}));

jest.mock('components/bootstrap', () => ({
  Input: mockComponent('InputMock'),
}));

jest.mock('components/lookup-tables', () => ({
  DataAdapterForm: mockComponent('DataAdapterFormMock'),
}));

jest.mock('graylog-web-plugin/plugin', () => ({
  PluginStore: {
    exports: () => ([
      {
        type: 'someType',
        displayName: 'Some Mocked Data Adapter Type',
      },
    ]),
  },
}));

describe('<DataAdapterCreate />', () => {
  it('should render with empty parameters', () => {
    const callback = () => {};
    const types = {};
    const wrapper = mount(<DataAdapterCreate saved={callback} types={types} />);

    expect(wrapper).toExist();
  });

  it('should render for types with defined frontend components', () => {
    const callback = () => {};
    const types = {
      someType: {
        type: 'someType',
      },
    };
    const wrapper = mount(<DataAdapterCreate saved={callback} types={types} />);

    expect(wrapper).toExist();
  });

  describe('with mocked console.error', () => {
    // eslint-disable-next-line no-console
    const consoleError = console.error;

    beforeAll(() => {
      // eslint-disable-next-line no-console
      console.error = jest.fn();
    });

    afterAll(() => {
      // eslint-disable-next-line no-console
      console.error = consoleError;
    });

    it('should render for types without defined frontend components', () => {
      const callback = () => {};
      const types = {
        unknownType: {
          type: 'unknownType',
        },
      };
      const wrapper = mount(<DataAdapterCreate saved={callback} types={types} />);

      expect(wrapper).toExist();
      // eslint-disable-next-line no-console
      expect(console.error.mock.calls.length).toBe(1);
    });
  });
});
