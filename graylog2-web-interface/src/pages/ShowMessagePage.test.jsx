// @flow strict
import * as React from 'react';
import { cleanup, render, wait } from 'wrappedTestingLibrary';
import { StoreMock as MockStore } from 'helpers/mocking';
import ShowMessagePage from './ShowMessagePage';
import { message, event, input } from './ShowMessagePage.fixtures';

jest.mock('views/components/messagelist/MessageDetail',
  () => (props) => <span>{JSON.stringify(props, null, 2)}</span>);

const mockLoadMessage = jest.fn();
const mockGetInput = jest.fn();
const mockListNodes = jest.fn();
const mockListStreams = jest.fn(() => Promise.resolve([]));

jest.mock('injection/CombinedProvider', () => ({
  get: jest.fn((type) => ({
    Inputs: { InputsActions: { get: (...args) => mockGetInput(...args) }, InputsStore: {} },
    Nodes: {
      NodesActions: { list: (...args) => mockListNodes(...args) },
      NodesStore: MockStore(['listen', () => () => {}], ['getInitialState', () => ({ nodes: {} })]),
    },
    Messages: { MessagesActions: { loadMessage: (...args) => mockLoadMessage(...args) } },
    Streams: { StreamsStore: { listStreams: (...args) => mockListStreams(...args) } },
  }[type])),
}));

describe('ShowMessagePage', () => {
  afterEach(cleanup);
  it('triggers a node list refresh on mount', async () => {
    mockLoadMessage.mockImplementation(() => Promise.resolve(message));
    mockGetInput.mockImplementation(() => Promise.resolve(input));
    render(<ShowMessagePage params={{ index: 'graylog_5', messageId: '20f683d2-a874-11e9-8a11-0242ac130004' }} />);
    await wait(() => expect(mockListNodes).toHaveBeenCalled());
  });
  it('renders for generic message', async () => {
    mockLoadMessage.mockImplementation(() => Promise.resolve(message));
    mockGetInput.mockImplementation(() => Promise.resolve(input));
    const { container, queryByTestId } = render(<ShowMessagePage params={{ index: 'graylog_5', messageId: '20f683d2-a874-11e9-8a11-0242ac130004' }} />);

    await wait(() => expect(queryByTestId('spinner')).toBeNull());
    expect(container).toMatchSnapshot();
  });
  it('renders for generic event', async () => {
    mockLoadMessage.mockImplementation(() => Promise.resolve(event));
    mockGetInput.mockImplementation(() => Promise.resolve());
    const { container, queryByTestId } = render(<ShowMessagePage params={{ index: 'gl-events_0', messageId: '01DFZQ64CMGV30NT7DW2P7HQX2' }} />);

    await wait(() => expect(queryByTestId('spinner')).toBeNull());
    expect(container).toMatchSnapshot();
  });
});
