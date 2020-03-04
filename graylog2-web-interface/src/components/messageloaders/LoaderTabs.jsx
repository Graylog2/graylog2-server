import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';
import { Tab, Tabs, Col } from 'components/graylog';
import Immutable from 'immutable';

import StoreProvider from 'injection/StoreProvider';

import ActionsProvider from 'injection/ActionsProvider';

import MessageShow from 'components/search/MessageShow';
import MessageLoader from 'components/extractors/MessageLoader';
import RawMessageLoader from './RawMessageLoader';
import RecentMessageLoader from './RecentMessageLoader';

const InputsStore = StoreProvider.getStore('Inputs');
const StreamsStore = StoreProvider.getStore('Streams');
const InputsActions = ActionsProvider.getActions('Inputs');

const LoaderTabs = createReactClass({
  displayName: 'LoaderTabs',

  propTypes: {
    tabs: PropTypes.oneOfType([
      PropTypes.oneOf(['recent', 'messageId', 'raw']),
      PropTypes.arrayOf(PropTypes.oneOf(['recent', 'messageId', 'raw'])),
    ]),
    messageId: PropTypes.string,
    index: PropTypes.string,
    onMessageLoaded: PropTypes.func,
    selectedInputId: PropTypes.string,
    customFieldActions: PropTypes.node,
    disableMessagePreview: PropTypes.bool,
  },

  mixins: [Reflux.listenTo(InputsStore, '_formatInputs')],

  getDefaultProps() {
    return {
      tabs: ['recent', 'messageId'],
    };
  },

  getInitialState() {
    return {
      activeTab: undefined,
      message: undefined,
      inputs: undefined,
    };
  },

  componentDidMount() {
    this.loadData();
    if (this.props.messageId && this.props.index) {
      this.messageLoader.submit(this.props.messageId, this.props.index);
    }
  },

  onMessageLoaded(message) {
    this.setState({ message: message });
    if (this.props.onMessageLoaded) {
      this.props.onMessageLoaded(message);
    }
  },

  TAB_KEYS: {
    recent: 1,
    messageId: 2,
    raw: 3,
  },

  _formatInputs(state) {
    const inputs = InputsStore.inputsAsMap(state.inputs);
    this.setState({ inputs: Immutable.Map(inputs) });
  },

  loadData() {
    InputsActions.list();
    StreamsStore.listStreams().then((response) => {
      const streams = {};
      response.forEach((stream) => {
        streams[stream.id] = stream;
      });
      this.setState({ streams: Immutable.Map(streams) });
    });
  },

  _isTabVisible(tabKey) {
    return this.props.tabs === tabKey || this.props.tabs.indexOf(tabKey) !== -1;
  },

  _getActiveTab() {
    if (this.state.activeTab) {
      return this.state.activeTab;
    }

    if (this._isTabVisible('messageId') && this.props.messageId && this.props.index) {
      return this.TAB_KEYS.messageId;
    }

    if (this._isTabVisible('recent')) {
      return this.TAB_KEYS.recent;
    }
    if (this._isTabVisible('messageId')) {
      return this.TAB_KEYS.messageId;
    }
    return this.TAB_KEYS.raw;
  },

  _changeActiveTab(selectedTab) {
    if (this.state.activeTab !== selectedTab) {
      this.setState({ activeTab: selectedTab, message: undefined });
    }
  },

  _formatMessageLoaders() {
    const messageLoaders = [];

    if (this._isTabVisible('recent')) {
      messageLoaders.push(
        <Tab key="recent" eventKey={this.TAB_KEYS.recent} title="Recent Message" style={{ marginBottom: 10 }}>
          <RecentMessageLoader inputs={this.state.inputs}
                               selectedInputId={this.props.selectedInputId}
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
  },

  render() {
    let displayMessage;
    if (this.state.message && this.state.inputs && !this.props.disableMessagePreview) {
      displayMessage = (
        <Col md={12}>
          <MessageShow message={this.state.message}
                       inputs={this.state.inputs}
                       streams={this.state.streams}
                       customFieldActions={this.props.customFieldActions} />
        </Col>
      );
    }

    return (
      <div>
        <Tabs id="loaderTabs" activeKey={this._getActiveTab()} onSelect={this._changeActiveTab} animation={false}>
          {this._formatMessageLoaders()}
        </Tabs>
        {displayMessage}
      </div>
    );
  },
});

export default LoaderTabs;
