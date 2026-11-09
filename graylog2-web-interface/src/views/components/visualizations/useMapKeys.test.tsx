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
import * as React from 'react';
import Immutable from 'immutable';
import { renderHook, waitFor } from 'wrappedTestingLibrary/hooks';

import usePluginEntities from 'hooks/usePluginEntities';
import asMock from 'helpers/mocking/AsMock';
import FieldTypesContext from 'views/components/contexts/FieldTypesContext';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import FieldType from 'views/logic/fieldtypes/FieldType';

import useMapKeys from './useMapKeys';

jest.mock('hooks/useInputs', () => () => ({ 'input-1': { id: 'input-1', title: 'Syslog UDP' } }));

jest.mock('hooks/useNodeSummaries', () => () => ({
  'node-1': { short_node_id: 'abc123', hostname: 'graylog-01' },
}));

jest.mock('hooks/usePluginEntities');

const fetchForwarderInput = jest.fn((id: string) => Promise.resolve({ id, title: `Forwarder input ${id}` }));
const fetchForwarderNode = jest.fn((id: string) => Promise.resolve({ id, title: `Forwarder ${id}` }));
const pluginEntities = { forwarder: [{ fetchForwarderInput, fetchForwarderNode }] };

const fields = Immutable.List([
  new FieldTypeMapping('gl2_source_input', new FieldType('input', [], [])),
  new FieldTypeMapping('gl2_source_node', new FieldType('node', [], [])),
]);

const wrapper = ({ children }: React.PropsWithChildren) => (
  <FieldTypesContext.Provider value={{ all: fields, currentQuery: fields }}>{children}</FieldTypesContext.Provider>
);

const useLabels = (inputIds: Array<string>, nodeIds: Array<string>) => {
  const mapKeys = useMapKeys();

  return {
    inputs: inputIds.map((id) => mapKeys(id, 'gl2_source_input')),
    nodes: nodeIds.map((id) => mapKeys(id, 'gl2_source_node')),
  };
};

describe('useMapKeys', () => {
  beforeEach(() => {
    asMock(usePluginEntities).mockReturnValue([]);
  });

  it('resolves local inputs and nodes', () => {
    const { result } = renderHook(() => useLabels(['input-1', 'missing'], ['node-1', 'missing']), { wrapper });

    expect(result.current).toEqual({
      inputs: ['Syslog UDP', 'missing'],
      nodes: ['abc123 / graylog-01', 'missing'],
    });
  });

  it('resolves forwarder inputs and nodes through the forwarder plugin', async () => {
    asMock(usePluginEntities).mockImplementation((key: string) => pluginEntities[key] ?? []);

    const { result } = renderHook(() => useLabels(['input-1', 'fw-input-1'], ['node-1', 'fw-node-1']), { wrapper });

    await waitFor(() =>
      expect(result.current).toEqual({
        inputs: ['Syslog UDP', 'Forwarder input fw-input-1'],
        nodes: ['abc123 / graylog-01', 'Forwarder fw-node-1'],
      }),
    );

    expect(fetchForwarderInput).toHaveBeenCalledWith('fw-input-1');
    expect(fetchForwarderInput).not.toHaveBeenCalledWith('input-1');
    expect(fetchForwarderNode).toHaveBeenCalledWith('fw-node-1');
    expect(fetchForwarderNode).not.toHaveBeenCalledWith('node-1');
  });
});
