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
import styled, { css } from 'styled-components';

import { Input } from 'components/bootstrap';
import StoreProvider from 'injection/StoreProvider';
import UserNotification from 'util/UserNotification';

const StreamsStore = StoreProvider.getStore('Streams');

const StreamRuleConnector = styled.div(({ theme }) => css`
  margin-top: 10px;
  margin-bottom: 13px;

  label {
    font-size: ${theme.fonts.size.small};
  }

  .form-group {
    margin-bottom: 5px;
  }

  .radio {
    margin-top: 0;
    margin-bottom: 0;
  }

  input[type=radio] {
    margin-top: 2px;
    margin-bottom: 2px;
  }
`);

class MatchingTypeSwitcher extends React.Component {
  static propTypes = {
    stream: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired,
  };

  handleTypeChangeToAnd = () => {
    this.handleTypeChange('AND');
  }

  handleTypeChangeToOr = () => {
    this.handleTypeChange('OR');
  }

  handleTypeChange = (newValue) => {
    const { onChange, stream } = this.props;

    // eslint-disable-next-line no-alert
    if (window.confirm('You are about to change how rules are applied to this stream, do you want to continue? Changes will take effect immediately.')) {
      StreamsStore.update(stream.id, { matching_type: newValue }, (response) => {
        onChange();

        UserNotification.success(`Messages will now be routed into the stream when ${newValue === 'AND' ? 'all' : 'any'} rules are matched`,
          'Success');

        return response;
      });
    }
  }

  render() {
    const { stream } = this.props;

    return (
      <StreamRuleConnector>
        <div>
          <Input id="streamrule-and-connector"
                 type="radio"
                 label="A message must match all of the following rules"
                 checked={stream.matching_type === 'AND'}
                 onChange={this.handleTypeChangeToAnd} />
          <Input id="streamrule-or-connector"
                 type="radio"
                 label="A message must match at least one of the following rules"
                 checked={stream.matching_type === 'OR'}
                 onChange={this.handleTypeChangeToOr} />
        </div>
      </StreamRuleConnector>
    );
  }
}

export default MatchingTypeSwitcher;
