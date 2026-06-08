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

import { Col, Tabs } from 'components/bootstrap';
import MessageShow from 'components/search/MessageShow';
import MessageLoader from 'components/extractors/MessageLoader';
import StreamsStore from 'stores/streams/StreamsStore';
import useInputsList, { inputsAsMap } from 'hooks/useInputs';
import type { Message } from 'views/components/messagelist/Types';
import type { Stream } from 'logic/streams/types';

import RawMessageLoader from './RawMessageLoader';
import RecentMessageLoader from './RecentMessageLoader';

type TabType = 'raw' | 'recent' | 'messageId';
type LoaderTabsProps = {
  tabs?: TabType[];
  messageId?: string;
  index?: string;
  onMessageLoaded?: (...args: any[]) => void;
  selectedInputId?: string;
  customFieldActions?: React.ReactElement;
  inputs?: any;
};

type TabDefinition = {
  key: TabType;
  title: string;
  content: React.ReactNode;
};

class LoaderTabs extends React.Component<
  LoaderTabsProps,
  {
    activeTab: TabType | undefined;
    message: Message | undefined;
    streams?: Immutable.Map<string, Stream>;
  }
> {
  static defaultProps: Partial<LoaderTabsProps> = {
    tabs: ['recent', 'messageId'],
    index: undefined,
    messageId: undefined,
    onMessageLoaded: undefined,
    selectedInputId: undefined,
    customFieldActions: undefined,
    inputs: undefined,
  };

  constructor(props: LoaderTabsProps) {
    super(props);

    this.state = {
      activeTab: undefined,
      message: undefined,
    };
  }

  componentDidMount() {
    this.loadData();
  }

  TAB_KEYS: Record<TabType, TabType> = {
    recent: 'recent',
    messageId: 'messageId',
    raw: 'raw',
  };

  onMessageLoaded = (message: Message) => {
    this.setState({ message });
    const { onMessageLoaded } = this.props;

    if (onMessageLoaded) {
      onMessageLoaded(message);
    }
  };

  loadData = () => {
    StreamsStore.listStreams().then((response: Array<Stream>) => {
      const streams = {};

      response.forEach((stream) => {
        streams[stream.id] = stream;
      });

      this.setState({ streams: Immutable.Map(streams) });
    });
  };

  _isTabVisible = (tabKey: TabType) => {
    const { tabs } = this.props;

    return tabs.indexOf(tabKey) !== -1;
  };

  _getActiveTab = (): TabType => {
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

  _changeActiveTab = (selectedTab: string | null) => {
    const { activeTab } = this.state;

    if (activeTab !== selectedTab) {
      this.setState({ activeTab: selectedTab as TabType, message: undefined });
    }
  };

  _getTabDefinitions = (): TabDefinition[] => {
    const { inputs, selectedInputId, messageId, index } = this.props;
    const defs: TabDefinition[] = [];

    if (this._isTabVisible('recent')) {
      defs.push({
        key: 'recent',
        title: 'Recent Message',
        content: (
          <RecentMessageLoader
            inputs={inputs}
            selectedInputId={selectedInputId}
            onMessageLoaded={this.onMessageLoaded}
          />
        ),
      });
    }

    if (this._isTabVisible('messageId')) {
      defs.push({
        key: 'messageId',
        title: 'Message ID',
        content: (
          <>
            <div style={{ marginTop: 5, marginBottom: 15 }}>
              Please provide the id and index of the message that you want to load in this form:
            </div>
            <MessageLoader
              messageId={messageId}
              index={index}
              onMessageLoaded={this.onMessageLoaded}
              hidden={false}
              hideText
            />
          </>
        ),
      });
    }

    if (this._isTabVisible('raw')) {
      defs.push({
        key: 'raw',
        title: 'Raw Message',
        content: (
          <>
            <div style={{ marginTop: 5, marginBottom: 15 }}>
              Load a message from text, as if it was sent by a log source.
            </div>
            <RawMessageLoader onMessageLoaded={this.onMessageLoaded} />
          </>
        ),
      });
    }

    return defs;
  };

  render() {
    const { streams, message } = this.state;
    const { customFieldActions, inputs } = this.props;
    const tabDefs = this._getTabDefinitions();

    const displayMessage =
      message && inputs ? (
        <Col md={12}>
          <MessageShow message={message} inputs={inputs} streams={streams} customFieldActions={customFieldActions} />
        </Col>
      ) : null;

    return (
      <div>
        <Tabs value={this._getActiveTab()} onChange={this._changeActiveTab}>
          <Tabs.List>
            {tabDefs.map(({ key, title }) => (
              <Tabs.Tab key={key} value={key}>
                {title}
              </Tabs.Tab>
            ))}
          </Tabs.List>
          {tabDefs.map(({ key, content }) => (
            <Tabs.Panel key={key} value={key} style={{ marginBottom: 10 }}>
              {content}
            </Tabs.Panel>
          ))}
        </Tabs>
        {displayMessage}
      </div>
    );
  }
}

const LoaderTabsWrapper = (props: Omit<LoaderTabsProps, 'inputs'>) => {
  const { data: inputsList } = useInputsList();
  const inputs = inputsList ? Immutable.Map(inputsAsMap(inputsList)) : undefined;

  return <LoaderTabs {...props} inputs={inputs} />;
};

export default LoaderTabsWrapper;
