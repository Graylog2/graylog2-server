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
import { render, screen, within } from 'wrappedTestingLibrary';
import * as Immutable from 'immutable';
import { PluginStore } from 'graylog-web-plugin/plugin';

import { MockStore } from 'helpers/mocking';
import type { Input } from 'components/messageloaders/Types';

import FormatReceivedBy from './FormatReceivedBy';

jest.mock('stores/nodes/NodesStore', () => ({
  NodesStore: MockStore(['getInitialState', () => ({ nodes: { existingNode: { short_node_id: 'foobar', hostname: 'existing.node' } } })]),
}));

type ForwarderReceivedByProps = {
  inputId: string,
  forwarderNodeId: string,
};

describe('FormatReceivedBy', () => {
  const inputs = Immutable.Map<string, Input>({
    bar: {
      title: 'My awesome input',
    },
  });

  it('shows that input is deleted if it is unknown', async () => {
    render(<FormatReceivedBy isLocalNode inputs={Immutable.Map()} sourceNodeId="foo" sourceInputId="bar" />);
    await screen.findByText('deleted input');
  });

  it('shows that node is stopped if it is unknown', async () => {
    render(<FormatReceivedBy isLocalNode inputs={Immutable.Map()} sourceNodeId="foo" sourceInputId="bar" />);
    await screen.findByText('stopped node');
  });

  it('shows input information if present', async () => {
    render(<FormatReceivedBy isLocalNode inputs={inputs} sourceNodeId="foo" sourceInputId="bar" />);
    await screen.findByText('My awesome input');
  });

  it('shows node information if present', async () => {
    render(<FormatReceivedBy isLocalNode inputs={Immutable.Map()} sourceNodeId="existingNode" sourceInputId="bar" />);

    const nodeLink = await screen.findByRole('link', { name: /existing.node/ }) as HTMLAnchorElement;

    expect(nodeLink.href).toEqual('http://localhost/system/nodes/existingNode');
    expect(within(nodeLink).getByText('foobar')).not.toBeNull();
  });

  describe('allows overriding node information through plugin', () => {
    const ForwarderReceivedBy = ({ inputId, forwarderNodeId }: ForwarderReceivedByProps) => <span>Mighty plugin magic: {inputId}/{forwarderNodeId}</span>;
    const isLocalNode = jest.fn(() => Promise.resolve(false));
    const pluginManifest = {
      exports: {
        forwarder: [{
          isLocalNode,
          ForwarderReceivedBy,
          messageLoaders: { ForwarderInputDropdown: () => <></> },
        }],
      },
    };

    beforeEach(() => PluginStore.register(pluginManifest));

    afterEach(() => PluginStore.unregister(pluginManifest));

    it('with correct definition', async () => {
      render(<FormatReceivedBy isLocalNode={false} inputs={inputs} sourceNodeId="foo" sourceInputId="bar" />);
      await screen.findByText('Mighty plugin magic: bar/foo');
    });

    it('but handles exception being thrown in `isLocalNode`', async () => {
      render(<FormatReceivedBy isLocalNode inputs={inputs} sourceNodeId="foo" sourceInputId="bar" />);
      await screen.findByText('stopped node');
    });
  });
});
