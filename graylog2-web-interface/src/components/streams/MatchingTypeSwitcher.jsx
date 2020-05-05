import PropTypes from 'prop-types';
import React from 'react';
import styled from 'styled-components';

import { Input } from 'components/bootstrap';

import StoreProvider from 'injection/StoreProvider';
import UserNotification from 'util/UserNotification';

const StreamsStore = StoreProvider.getStore('Streams');

const StreamRuleConnector = styled.div`
  margin-top: 10px;
  margin-bottom: 13px;

  label {
    font-size: 14px;
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
`;

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
    if (window.confirm('You are about to change how rules are applied to this stream, do you want to continue? Changes will take effect immediately.')) {
      StreamsStore.update(this.props.stream.id, { matching_type: newValue }, (response) => {
        this.props.onChange();
        UserNotification.success(`Messages will now be routed into the stream when ${newValue === 'AND' ? 'all' : 'any'} rules are matched`,
          'Success');
        return response;
      });
    }
  }

  render() {
    return (
      <StreamRuleConnector>
        <div>
          <Input id="streamrule-and-connector"
                 type="radio"
                 label="A message must match all of the following rules"
                 checked={this.props.stream.matching_type === 'AND'}
                 onChange={this.handleTypeChangeToAnd} />
          <Input id="streamrule-or-connector"
                 type="radio"
                 label="A message must match at least one of the following rules"
                 checked={this.props.stream.matching_type === 'OR'}
                 onChange={this.handleTypeChangeToOr} />
        </div>
      </StreamRuleConnector>
    );
  }
}

export default MatchingTypeSwitcher;
