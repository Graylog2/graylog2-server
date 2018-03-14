import PropTypes from 'prop-types';
import React from 'react';
import { Button } from 'react-bootstrap';

import UserPreferencesModal from 'components/users/UserPreferencesModal';

class UserPreferencesButton extends React.Component {
  static propTypes = {
    userName: PropTypes.string.isRequired,
  };

  onClick = () => {
    this.refs.userPreferencesModal.openModal();
  };

  render() {
    return (
      <span>
        <Button onClick={this.onClick} bsStyle="success">User preferences</Button>
        <UserPreferencesModal ref="userPreferencesModal" userName={this.props.userName} />
      </span>
    );
  }
}

export default UserPreferencesButton;
