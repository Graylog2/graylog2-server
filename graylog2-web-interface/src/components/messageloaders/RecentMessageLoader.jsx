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
import PropTypes from 'prop-types';
import React from 'react';

import InputDropdown from 'components/inputs/InputDropdown';
import UserNotification from 'util/UserNotification';
import StoreProvider from 'injection/StoreProvider';

const UniversalSearchStore = StoreProvider.getStore('UniversalSearch');

class RecentMessageLoader extends React.Component {
  static propTypes = {
    inputs: PropTypes.object,
    onMessageLoaded: PropTypes.func.isRequired,
    selectedInputId: PropTypes.string,
  };

  state = {
    loading: false,
  };

  onClick = (inputId) => {
    const input = this.props.inputs.get(inputId);

    if (!input) {
      UserNotification.error(`Invalid input selected: ${inputId}`,
        `Could not load message from invalid Input ${inputId}`);
    }

    this.setState({ loading: true });
    const promise = UniversalSearchStore.search('relative', `gl2_source_input:${inputId} OR gl2_source_radio_input:${inputId}`,
      { range: 3600 }, undefined, 1, undefined, undefined, undefined, false);

    promise.then((response) => {
      if (response.total_results > 0) {
        this.props.onMessageLoaded(response.messages[0]);
      } else {
        UserNotification.error('Input did not return a recent message.');
        this.props.onMessageLoaded(undefined);
      }
    });

    promise.finally(() => this.setState({ loading: false }));
  };

  render() {
    let helpMessage;

    if (this.props.selectedInputId) {
      helpMessage = 'Click on "Load Message" to load the most recent message received by this input within the last hour.';
    } else {
      helpMessage = 'Select an Input from the list below and click "Load Message" to load the most recent message received by this input within the last hour.';
    }

    return (
      <div style={{ marginTop: 5 }}>
        {helpMessage}
        <InputDropdown inputs={this.props.inputs}
                       preselectedInputId={this.props.selectedInputId}
                       onLoadMessage={this.onClick}
                       title={this.state.loading ? 'Loading message...' : 'Load Message'}
                       disabled={this.state.loading} />
      </div>
    );
  }
}

export default RecentMessageLoader;
