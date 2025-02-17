import * as React from 'react';
import { render, screen } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

import MockStore from 'helpers/mocking/StoreMock';

import InputThroughput from './InputThroughput';

jest.mock('stores/nodes/NodesStore', () => ({
  NodesActions: {
    list: jest.fn(),
  },
  NodesStore: MockStore([
    'getInitialState',
    () => ({
      nodes: {
        '3b37ead8-ff96-4af3-a1e1-97b07d8241c1': {
          cluster_id: 'clusterid',
          short_node_id: 'foobar',
          hostname: 'existing.node',
        },
      },
    }),
  ]),
}));

const input = {
  'title': 'GELF HTTP In',
  'global': true,
  'name': 'GELF HTTP',
  'content_pack': null,
  'created_at': '2024-03-27T10:43:02.547Z',
  'type': 'org.graylog2.inputs.gelf.http.GELFHttpInput',
  'creator_user_id': 'dennis',
  'attributes': {
    'idle_writer_timeout': 60,
    'recv_buffer_size': 1048576,
    'max_chunk_size': 65536,
    'tcp_keepalive': false,
    'number_worker_threads': 12,
    'enable_cors': true,
    'tls_client_auth_cert_file': '',
    'bind_address': '0.0.0.0',
    'tls_cert_file': '',
    'decompress_size_limit': 8388608,
    'port': 12201,
    'tls_key_file': '',
    'tls_enable': false,
    'tls_key_password': '',
    'tls_client_auth': 'disabled',
    'override_source': null,
    'charset_name': 'UTF-8',
    'enable_bulk_receiving': false,
  },
  'static_fields': {},
  'node': '3b37ead8-ff96-4af3-a1e1-97b07d8241c1',
  'id': '605c80c45c06ac1399999d0a',
};
const metrics = {
  '3b37ead8-ff96-4af3-a1e1-97b07d8241c1': {
    'org.graylog2.inputs.gelf.http.GELFHttpInput.605c80c45c06ac1399999d0a.incomingMessages': {
      'full_name': 'org.graylog2.inputs.gelf.http.GELFHttpInput.605c80c45c06ac1399999d0a.incomingMessages',
      'metric': {
        'rate': {
          'total': 1328,
          'mean': 3,
          'five_minute': 3,
          'fifteen_minute': 5,
          'one_minute': 2,
        },
        'rate_unit': 'events/second',
      },
      'name': 'incomingMessages',
      'type': 'meter',
    },
    'org.graylog2.inputs.gelf.http.GELFHttpInput.605c80c45c06ac1399999d0a.emptyMessages': {
      'full_name': 'org.graylog2.inputs.gelf.http.GELFHttpInput.605c80c45c06ac1399999d0a.emptyMessages',
      'metric': {
        'count': 0,
      },
      'name': 'emptyMessages',
      'type': 'counter',
    },
    'org.graylog2.inputs.gelf.http.GELFHttpInput.605c80c45c06ac1399999d0a.open_connections': {
      'full_name': 'org.graylog2.inputs.gelf.http.GELFHttpInput.605c80c45c06ac1399999d0a.open_connections',
      'metric': {
        'value': 5,
      },
      'name': 'open_connections',
      'type': 'gauge',
    },
    'org.graylog2.inputs.gelf.http.GELFHttpInput.605c80c45c06ac1399999d0a.total_connections': {
      'full_name': 'org.graylog2.inputs.gelf.http.GELFHttpInput.605c80c45c06ac1399999d0a.total_connections',
      'metric': {
        'value': 312,
      },
      'name': 'total_connections',
      'type': 'gauge',
    },
    'org.graylog2.inputs.gelf.http.GELFHttpInput.605c80c45c06ac1399999d0a.written_bytes_1sec': {
      'full_name': 'org.graylog2.inputs.gelf.http.GELFHttpInput.605c80c45c06ac1399999d0a.written_bytes_1sec',
      'metric': {
        'value': 3728,
      },
      'name': 'written_bytes_1sec',
      'type': 'gauge',
    },
    'org.graylog2.inputs.gelf.http.GELFHttpInput.605c80c45c06ac1399999d0a.written_bytes_total': {
      'full_name': 'org.graylog2.inputs.gelf.http.GELFHttpInput.605c80c45c06ac1399999d0a.written_bytes_total',
      'metric': {
        'value': 858291,
      },
      'name': 'written_bytes_total',
      'type': 'gauge',
    },
    'org.graylog2.inputs.gelf.http.GELFHttpInput.605c80c45c06ac1399999d0a.read_bytes_1sec': {
      'full_name': 'org.graylog2.inputs.gelf.http.GELFHttpInput.605c80c45c06ac1399999d0a.read_bytes_1sec',
      'metric': {
        'value': 31238,
      },
      'name': 'read_bytes_1sec',
      'type': 'gauge',
    },
    'org.graylog2.inputs.gelf.http.GELFHttpInput.605c80c45c06ac1399999d0a.read_bytes_total': {
      'full_name': 'org.graylog2.inputs.gelf.http.GELFHttpInput.605c80c45c06ac1399999d0a.read_bytes_total',
      'metric': {
        'value': 2383219,
      },
      'name': 'read_bytes_total',
      'type': 'gauge',
    },
  },
} as const;

describe('InputThroughput', () => {
  it('expands details section on click', async () => {
    render(<InputThroughput input={input} metrics={metrics} />);
    const detailsLink = await screen.findByRole('link', { name: /show details/i });
    await screen.findByRole('heading', { name: /Throughput \/ Metrics/i });

    expect(screen.queryByText('foobar / existing.node')).not.toBeInTheDocument();

    userEvent.click(detailsLink);

    await screen.findByText('foobar / existing.node');
  });
});
