import React, { useRef } from 'react';
import PropTypes from 'prop-types';

import { Button } from 'components/graylog';
import UserPreferencesModal from 'components/users/UserPreferencesModal';

const UserPreferencesButton = ({ userName }) => {
  const userPreferencesModal = useRef();

  const onClick = () => {
    userPreferencesModal.current.openModal();
  };

  return (
    <span>
      <Button onClick={onClick}
              bsStyle="success"
              data-testid="user-preferences-button">User preferences
      </Button>
      <UserPreferencesModal ref={userPreferencesModal} userName={userName} />
    </span>
  );
};

UserPreferencesButton.propTypes = {
  userName: PropTypes.string.isRequired,
};

export default UserPreferencesButton;
