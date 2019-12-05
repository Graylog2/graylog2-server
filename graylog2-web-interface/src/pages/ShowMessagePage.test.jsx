import * as React from 'react';
import { mount } from 'enzyme';
import { StoreMock } from 'helpers/mocking';

jest.mock('components/search/MessageDetail', () => 'message-detail');
// We need to mock the spinner here, because it's a hook component, which gets returned by the ShowMessagePage.
// This does not work in combination with the require() we are using in beforeEach()
jest.mock('components/common/Spinner', () => 'div');

const message = {
  id: '20f683d2-a874-11e9-8a11-0242ac130004',
  timestamp: 1563362433,
  filtered_fields: {
    level: 6,
    source: 'babbage',
    message: 'babbage 30ac6e35e442[27354]: [2019-07-17T09:20:33,415][WARN ][o.e.d.c.ParseField       ] [Mc1oQWu] Deprecated field [split_on_whitespace] used, replaced by [This setting is ignored, the parser always splits on operator]',
    hostname: 'babbage',
    facility: 'user-level',
    timestamp: '2019-07-17T11:20:33.000Z',
  },
  formatted_fields: {
    level: 6,
    source: 'babbage',
    message: 'babbage 30ac6e35e442[27354]: [2019-07-17T09:20:33,415][WARN ][o.e.d.c.ParseField       ] [Mc1oQWu] Deprecated field [split_on_whitespace] used, replaced by [This setting is ignored, the parser always splits on operator]',
    hostname: 'babbage',
    facility: 'user-level',
    timestamp: '2019-07-17T11:20:33.000Z',
  },
  fields: {
    level: 6,
    gl2_remote_ip: '192.168.1.47',
    gl2_remote_port: 35024,
    streams: ['000000000000000000000001'],
    gl2_message_id: '01DFZKQF4D3642JY91FM6Z1WQG',
    source: 'babbage',
    message: 'babbage 30ac6e35e442[27354]: [2019-07-17T09:20:33,415][WARN ][o.e.d.c.ParseField       ] [Mc1oQWu] Deprecated field [split_on_whitespace] used, replaced by [This setting is ignored, the parser always splits on operator]',
    gl2_source_input: '5c26a37b3885e50480aa12a2',
    hostname: 'babbage',
    gl2_source_node: '4c0cbe7b-c51a-4617-bb50-ea01fe6dbfd0',
    _id: '20f683d2-a874-11e9-8a11-0242ac130004',
    facility: 'user-level',
    timestamp: '2019-07-17T11:20:33.000Z',
  },
  index: 'graylog_5',
  source_node_id: '4c0cbe7b-c51a-4617-bb50-ea01fe6dbfd0',
  source_input_id: '5c26a37b3885e50480aa12a2',
  stream_ids: [],
  highlight_ranges: {},
};

const input = {
  title: 'syslog udp',
  global: true,
  name: 'Syslog UDP',
  content_pack: null,
  created_at: '2019-07-15T07:25:12.397Z',
  type: 'org.graylog2.inputs.syslog.udp.SyslogUDPInput',
  creator_user_id: 'admin',
  attributes: {
    expand_structured_data: false,
    recv_buffer_size: 262144,
    port: 12514,
    number_worker_threads: 8,
    override_source: null,
    force_rdns: false,
    allow_override_date: true,
    bind_address: '0.0.0.0',
    store_full_message: false,
  },
  static_fields: {},
  node: '4c0cbe7b-c51a-4617-bb50-ea01fe6dbfd0',
  id: '5c26a37b3885e50480aa12a2',
};

const event = {
  id: '01DFZQ64CMGV30NT7DW2P7HQX2',
  timestamp: 1563357012,
  filtered_fields: {
    timestamp_processing: '2019-07-17 10:20:59.668',
    origin_context: 'urn:graylog:message:es:netflow_13:459cb2f1-a878-11e9-8a11-0242ac130004',
    source: 'localhost',
    message: 'SSH Brute Force',
    priority: 2,
    key_tuple: [],
    alert: false,
    event_definition_type: 'aggregation-v1',
    event_definition_id: '5d2d8350e117dc4659d2dfcc',
    id: '01DFZQ64CMGV30NT7DW2P7HQX2',
    fields: { ssh_target: '10.0.0.82' },
    timestamp: '2019-07-17T09:50:12.886Z',
  },
  formatted_fields: {
    timestamp_processing: '2019-07-17 10:20:59.668',
    origin_context: 'urn:graylog:message:es:netflow_13:459cb2f1-a878-11e9-8a11-0242ac130004',
    source: 'localhost',
    message: 'SSH Brute Force',
    priority: 2,
    key_tuple: [],
    alert: false,
    event_definition_type: 'aggregation-v1',
    event_definition_id: '5d2d8350e117dc4659d2dfcc',
    id: '01DFZQ64CMGV30NT7DW2P7HQX2',
    fields: { ssh_target: '10.0.0.82' },
    timestamp: '2019-07-17T09:50:12.886Z',
  },
  fields: {
    timestamp_processing: '2019-07-17 10:20:59.668',
    origin_context: 'urn:graylog:message:es:netflow_13:459cb2f1-a878-11e9-8a11-0242ac130004',
    streams: ['000000000000000000000002'],
    source: 'localhost',
    message: 'SSH Brute Force',
    priority: 2,
    key_tuple: [],
    alert: false,
    event_definition_type: 'aggregation-v1',
    event_definition_id: '5d2d8350e117dc4659d2dfcc',
    _id: '01DFZQ64CMGV30NT7DW2P7HQX2',
    id: '01DFZQ64CMGV30NT7DW2P7HQX2',
    fields: { ssh_target: '10.0.0.82' },
    timestamp: '2019-07-17T09:50:12.886Z',
  },
  index: 'gl-events_0',
  stream_ids: [],
  highlight_ranges: {},
};

describe('ShowMessagePage', () => {
  let ShowMessagePage;
  let mockMappings;
  beforeEach(() => {
    jest.resetModules();
    mockMappings = {
      NodesActions: {
        list: jest.fn(),
      },
      InputsActions: {},
      MessagesActions: {},
      NodesStore: {
        getInitialState: jest.fn(() => ({ nodes: [] })),
        listen: jest.fn(),
      },
      StreamsStore: {
        listStreams: jest.fn(() => Promise.resolve()),
      },
      InputsStore: {},
    };

    jest.doMock('injection/CombinedProvider', () => ({
      get: key => ({
        [`${key}Store`]: mockMappings[`${key}Store`] || StoreMock(),
        [`${key}Actions`]: mockMappings[`${key}Actions`] || {},
      }),
    }));

    // eslint-disable-next-line global-require
    ShowMessagePage = require('./ShowMessagePage');
  });

  it('triggers a node list refresh on mount', () => {
    mockMappings.MessagesActions.loadMessage = jest.fn(() => Promise.resolve(message));
    mockMappings.InputsActions.get = jest.fn(() => Promise.resolve(input));
    mount(<ShowMessagePage searchConfig={{}} params={{ index: 'graylog_5', messageId: '20f683d2-a874-11e9-8a11-0242ac130004' }} />);
    expect(mockMappings.NodesActions.list).toHaveBeenCalled();
  });
  it('renders for generic message', (done) => {
    mockMappings.MessagesActions.loadMessage = jest.fn(() => Promise.resolve(message));
    mockMappings.InputsActions.get = jest.fn(() => Promise.resolve(input));
    const wrapper = mount(<ShowMessagePage searchConfig={{}} params={{ index: 'graylog_5', messageId: '20f683d2-a874-11e9-8a11-0242ac130004' }} />);
    setImmediate(() => {
      expect(wrapper.update()).toMatchSnapshot();
      done();
    });
  });
  it('renders for generic event', (done) => {
    mockMappings.MessagesActions.loadMessage = jest.fn(() => Promise.resolve(event));
    mockMappings.InputsActions.get = jest.fn(() => Promise.resolve());
    const wrapper = mount(<ShowMessagePage searchConfig={{}} params={{ index: 'gl-events_0', messageId: '01DFZQ64CMGV30NT7DW2P7HQX2' }} />);
    setImmediate(() => {
      expect(wrapper.update()).toMatchSnapshot();
      done();
    });
  });
});
