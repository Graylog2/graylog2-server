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
import * as Immutable from 'immutable';

import { Col, Tab, Tabs } from 'components/bootstrap';
import connect from 'stores/connect';
import MessageShow from 'components/search/MessageShow';
import MessageLoader from 'components/extractors/MessageLoader';
import StreamsStore from 'stores/streams/StreamsStore';
import { InputsActions, InputsStore } from 'stores/inputs/InputsStore';

import RawMessageLoader from './RawMessageLoader';
import RecentMessageLoader from './RecentMessageLoader';

type LoaderTabsProps = {
  tabs?: 'recent' | 'messageId' | 'raw' | 'recent' | 'messageId' | 'raw'[];
  messageId?: string;
  index?: string;
  onMessageLoaded?: (...args: any[]) => void;
  selectedInputId?: string;
  customFieldActions?: React.ReactNode;
  inputs?: any;
};

class LoaderTabs extends React.Component<LoaderTabsProps, {
  [key: string]: any;
}> {
  static defaultProps = {
    tabs: ['recent', 'messageId'],
    index: undefined,
    messageId: undefined,
    onMessageLoaded: undefined,
    selectedInputId: undefined,
    customFieldActions: undefined,
    inputs: undefined,
  };

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
      const { inputs, selectedInputId } = this.props;

      messageLoaders.push(
        <Tab key="recent" eventKey={this.TAB_KEYS.recent} title="Recent Message" style={{ marginBottom: 10 }}>
          <RecentMessageLoader inputs={inputs}
                               selectedInputId={selectedInputId}
                               onMessageLoaded={this.onMessageLoaded} />
        </Tab>,
      );
    }

    if (this._isTabVisible('messageId')) {
      const { messageId, index } = this.props;

      messageLoaders.push(
        <Tab key="messageId" eventKey={this.TAB_KEYS.messageId} title="Message ID" style={{ marginBottom: 10 }}>
          <div style={{ marginTop: 5, marginBottom: 15 }}>
            Please provide the id and index of the message that you want to load in this form:
          </div>
          <MessageLoader messageId={messageId}
                         index={index}
                         onMessageLoaded={this.onMessageLoaded}
                         hidden={false}
                         hideText />
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

export default connect(
  LoaderTabs,
  { inputs: InputsStore },
  ({ inputs: { inputs } }) => ({ inputs: inputs ? Immutable.Map(InputsStore.inputsAsMap(inputs)) : undefined }),
);
