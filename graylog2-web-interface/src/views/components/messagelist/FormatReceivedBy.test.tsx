import * as React from 'react';
import { render, screen, within } from 'wrappedTestingLibrary';
import * as Immutable from 'immutable';
import { MockCombinedProvider, MockStore } from 'helpers/mocking';
import { PluginStore } from 'graylog-web-plugin/plugin';

import { Input } from 'components/messageloaders/Types';

import FormatReceivedBy from './FormatReceivedBy';

jest.mock('injection/CombinedProvider', () => new MockCombinedProvider({
  Nodes: {
    NodesStore: MockStore(['getInitialState', () => ({ nodes: { existingNode: { short_node_id: 'foobar', hostname: 'existing.node' } } })]),
  },
}));

type ForwarderReceivedByProps = {
  inputId: string,
  forwarderNodeId: string,
};

describe('FormatReceivedBy', () => {
  it('shows that input is deleted if it is unknown', async () => {
    render(<FormatReceivedBy inputs={Immutable.Map()} sourceNodeId="foo" sourceInputId="bar" />);
    await screen.findByText('deleted input');
  });

  it('shows that node is stopped if it is unknown', async () => {
    render(<FormatReceivedBy inputs={Immutable.Map()} sourceNodeId="foo" sourceInputId="bar" />);
    await screen.findByText('stopped node');
  });

  it('shows input information if present', async () => {
    const inputs = Immutable.Map<string, Input>({
      bar: {
        title: 'My awesome input',
      },
    });
    render(<FormatReceivedBy inputs={inputs} sourceNodeId="foo" sourceInputId="bar" />);
    await screen.findByText('My awesome input');
  });

  it('shows node information if present', async () => {
    render(<FormatReceivedBy inputs={Immutable.Map()} sourceNodeId="existingNode" sourceInputId="bar" />);

    const nodeLink = await screen.findByRole('link', { name: /existing.node/ }) as HTMLAnchorElement;

    expect(nodeLink.href).toEqual('http://localhost/system/nodes/existingNode');
    expect(within(nodeLink).getByText('foobar')).not.toBeNull();
  });

  it('allows overriding node information through plugin', async () => {
    const ForwarderReceivedBy = ({ inputId, forwarderNodeId }: ForwarderReceivedByProps) => <span>Mighty plugin magic: {inputId}/{forwarderNodeId}</span>;
    const pluginManifest = {
      exports: {
        forwarder: [{
          isLocalNode: () => Promise.resolve(false),
          ForwarderReceivedBy,
          messageLoaders: { ForwarderInputDropdown: () => <></> },
        }],
      },
    };
    PluginStore.register(pluginManifest);
    const inputs = Immutable.Map<string, Input>({
      bar: {
        title: 'My awesome input',
      },
    });

    render(<FormatReceivedBy inputs={inputs} sourceNodeId="foo" sourceInputId="bar" />);
    await screen.findByText('Mighty plugin magic: bar/foo');
  });
});
