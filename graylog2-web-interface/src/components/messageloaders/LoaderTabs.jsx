import * as React from 'react';
import PropTypes from 'prop-types';
import { Col, Tab, Tabs } from 'components/graylog';
import * as Immutable from 'immutable';

import connect from 'stores/connect';
import StoreProvider from 'injection/StoreProvider';
import ActionsProvider from 'injection/ActionsProvider';

import MessageShow from 'components/search/MessageShow';
import MessageLoader from 'components/extractors/MessageLoader';
import RawMessageLoader from './RawMessageLoader';
import RecentMessageLoader from './RecentMessageLoader';

const InputsStore = StoreProvider.getStore('Inputs');
const StreamsStore = StoreProvider.getStore('Streams');
const InputsActions = ActionsProvider.getActions('Inputs');

class LoaderTabs extends React.Component {
  TAB_KEYS = {
    recent: 1,
    messageId: 2,
    raw: 3,
  };

  constructor(props) {
    super(props);

    this.state = {
      activeTab: undefined,
      message: undefined,
    };
  }

  componentDidMount() {
    this.loadData();
    const { messageId, index } = this.props;
    if (messageId && index) {
      this.messageLoader.submit(messageId, index);
    }
  }

  onMessageLoaded = (message) => {
    this.setState({ message });
    const { onMessageLoaded } = this.props;
    if (onMessageLoaded) {
      onMessageLoaded(message);
    }
  };

  loadData = () => {
    InputsActions.list();
    StreamsStore.listStreams().then((response) => {
      const streams = {};
      response.forEach((stream) => {
        streams[stream.id] = stream;
      });
      this.setState({ streams: Immutable.Map(streams) });
    });
  };

  _isTabVisible = (tabKey) => {
    const { tabs } = this.props;
    return tabs === tabKey || tabs.indexOf(tabKey) !== -1;
  };

  _getActiveTab = () => {
    const { activeTab } = this.state;
    if (activeTab) {
      return activeTab;
    }

    const { messageId, index } = this.props;
    if (this._isTabVisible('messageId') && messageId && index) {
      return this.TAB_KEYS.messageId;
    }

    if (this._isTabVisible('recent')) {
      return this.TAB_KEYS.recent;
    }
    if (this._isTabVisible('messageId')) {
      return this.TAB_KEYS.messageId;
    }
    return this.TAB_KEYS.raw;
  };

  _changeActiveTab = (selectedTab) => {
    const { activeTab } = this.state;
    if (activeTab !== selectedTab) {
      this.setState({ activeTab: selectedTab, message: undefined });
    }
  };

  _formatMessageLoaders = () => {
    const messageLoaders = [];

    if (this._isTabVisible('recent')) {
      const { inputs } = this.props;
      const { selectedInputId } = this.props;
      messageLoaders.push(
        <Tab key="recent" eventKey={this.TAB_KEYS.recent} title="Recent Message" style={{ marginBottom: 10 }}>
          <RecentMessageLoader inputs={inputs}
                               selectedInputId={selectedInputId}
                               onMessageLoaded={this.onMessageLoaded} />
        </Tab>,
      );
    }

    if (this._isTabVisible('messageId')) {
      messageLoaders.push(
        <Tab key="messageId" eventKey={this.TAB_KEYS.messageId} title="Message ID" style={{ marginBottom: 10 }}>
          <div style={{ marginTop: 5, marginBottom: 15 }}>
            Please provide the id and index of the message that you want to load in this form:
          </div>

          <MessageLoader ref={(messageLoader) => { this.messageLoader = messageLoader; }} onMessageLoaded={this.onMessageLoaded} hidden={false} hideText />
        </Tab>,
      );
    }

    if (this._isTabVisible('raw')) {
      messageLoaders.push(
        <Tab key="raw" eventKey={this.TAB_KEYS.raw} title="Raw Message" style={{ marginBottom: 10 }}>
          <div style={{ marginTop: 5, marginBottom: 15 }}>
            Load a message from text, as if it was sent by a log source.
          </div>

          <RawMessageLoader onMessageLoaded={this.onMessageLoaded} />
        </Tab>,
      );
    }

    return messageLoaders;
  };

  render() {
    const { streams, message } = this.state;
    const { customFieldActions, inputs } = this.props;
    const displayMessage = message && inputs
      ? (
        <Col md={12}>
          <MessageShow message={message}
                       inputs={inputs}
                       streams={streams}
                       customFieldActions={customFieldActions} />
        </Col>
      )
      : null;

    return (
      <div>
        <Tabs id="loaderTabs" activeKey={this._getActiveTab()} onSelect={this._changeActiveTab} animation={false}>
          {this._formatMessageLoaders()}
        </Tabs>
        {displayMessage}
      </div>
    );
  }
}

LoaderTabs.propTypes = {
  tabs: PropTypes.oneOfType([
    PropTypes.oneOf(['recent', 'messageId', 'raw']),
    PropTypes.arrayOf(PropTypes.oneOf(['recent', 'messageId', 'raw'])),
  ]),
  messageId: PropTypes.string,
  index: PropTypes.string,
  onMessageLoaded: PropTypes.func,
  selectedInputId: PropTypes.string,
  customFieldActions: PropTypes.node,
  inputs: PropTypes.object,
};

LoaderTabs.defaultProps = {
  tabs: ['recent', 'messageId'],
  index: undefined,
  messageId: undefined,
  onMessageLoaded: undefined,
  selectedInputId: undefined,
  customFieldActions: undefined,
  inputs: undefined,
};

export default connect(
  LoaderTabs,
  { inputs: InputsStore },
  ({ inputs: { inputs } }) => ({ inputs: inputs ? Immutable.Map(InputsStore.inputsAsMap(inputs)) : undefined }),
);
