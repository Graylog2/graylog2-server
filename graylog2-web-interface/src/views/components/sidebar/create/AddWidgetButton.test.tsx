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
import type { PluginExports } from 'graylog-web-plugin/plugin';
import { PluginStore } from 'graylog-web-plugin/plugin';

import { asMock } from 'helpers/mocking';
import useAppDispatch from 'stores/useAppDispatch';
import { createSearch } from 'fixtures/searches';
import mockDispatch from 'views/test/mockDispatch';
import type { RootState } from 'views/types';

import AddWidgetButton from './AddWidgetButton';

const mockAggregateActionHandler = jest.fn();
const mockAddMessageCountActionHandler = jest.fn();
const mockAddMessageTableActionHandler = jest.fn();

const MockCreateParameterDialog = () => {
  return <span>42</span>;
};

const bindings: PluginExports = {
  creators: [
    {
      type: 'preset',
      title: 'Message Count',
      func: mockAddMessageCountActionHandler,
    },
    {
      type: 'preset',
      title: 'Message Table',
      func: mockAddMessageTableActionHandler,
    },
    {
      type: 'generic',
      title: 'Aggregation',
      func: mockAggregateActionHandler,
    },
    {
      type: 'generic',
      title: 'Parameter',
      component: MockCreateParameterDialog,
    },
  ],

};

const plugin = {
  exports: bindings,
  metadata: {
    name: 'Dummy Plugin for Tests',
  },
};

jest.mock('stores/useAppDispatch');

describe('AddWidgetButton', () => {
  beforeEach(() => {
    const view = createSearch();
    const dispatch = mockDispatch({ view: { view, activeQuery: 'query-id-1' } } as RootState);
    asMock(useAppDispatch).mockReturnValue(dispatch);
    PluginStore.register(plugin);
  });

  afterEach(() => {
    PluginStore.unregister(plugin);
  });

  const onClick = jest.fn();

  it('contains menu items for all widget types', () => {
    const wrapper = mount(<AddWidgetButton onClick={onClick} />);

    ['Aggregation', 'Message Count', 'Message Table', 'Parameter']
      .forEach((title) => expect(wrapper.find(`button[children="${title}"]`)).toExist());
  });

  it('clicking on option to add aggregation calls AggregateActionHandler', () => {
    const wrapper = mount(<AddWidgetButton onClick={onClick} />);

    const addAggregation = wrapper.find('button[children="Aggregation"]');

    expect(addAggregation).toExist();

    addAggregation.simulate('click');

    expect(mockAggregateActionHandler).toHaveBeenCalled();
  });

  it('clicking on option to add message count calls AddMessageCountActionHandler', () => {
    const wrapper = mount(<AddWidgetButton onClick={onClick} />);

    const addMessageCount = wrapper.find('button[children="Message Count"]');

    expect(addMessageCount).toExist();

    addMessageCount.simulate('click');

    expect(mockAddMessageCountActionHandler).toHaveBeenCalled();
  });

  it('clicking on option to add message table calls AddMessageTableActionHandler', () => {
    const wrapper = mount(<AddWidgetButton onClick={onClick} />);

    const addMessageTable = wrapper.find('button[children="Message Table"]');

    expect(addMessageTable).toExist();

    addMessageTable.simulate('click');

    expect(mockAddMessageTableActionHandler).toHaveBeenCalled();
  });

  it('clicking on option to add a parameter renders MockCreateParameterDialog', () => {
    const wrapper = mount(<AddWidgetButton onClick={onClick} />);

    const addMessageTable = wrapper.find('button[children="Parameter"]');

    expect(addMessageTable).toExist();

    addMessageTable.simulate('click');

    wrapper.update();

    expect(wrapper.find('MockCreateParameterDialog')).toExist();
  });

  it('calling onClose from creator component removes it', () => {
    const wrapper = mount(<AddWidgetButton onClick={onClick} />);

    const addMessageTable = wrapper.find('button[children="Parameter"]');

    expect(addMessageTable).toExist();

    addMessageTable.simulate('click');

    wrapper.update();

    const mockCreateParameterDialog = wrapper.find('MockCreateParameterDialog');

    expect(mockCreateParameterDialog).toExist();

    const { onClose } = mockCreateParameterDialog.props() as { onClose: () => void };

    onClose();

    wrapper.update();

    expect(wrapper.find('MockCreateParameterDialog')).not.toExist();
  });
});
