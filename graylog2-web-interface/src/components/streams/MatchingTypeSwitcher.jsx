import PropTypes from 'prop-types';
import React from 'react';
import { Input } from 'components/bootstrap';

import StoreProvider from 'injection/StoreProvider';
import UserNotification from 'util/UserNotification';

const StreamsStore = StoreProvider.getStore('Streams');

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
      <div className="streamrule-connector-type-form">
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
      </div>
    );
  }
}

export default MatchingTypeSwitcher;
