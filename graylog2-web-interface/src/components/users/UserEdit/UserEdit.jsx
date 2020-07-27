// @flow strict
import * as React from 'react';
import { useContext } from 'react';

import { UsersActions } from 'stores/users/UsersStore';
import CurrentUserContext from 'contexts/CurrentUserContext';
import { Spinner } from 'components/common';
import UserNotification from 'util/UserNotification';
import User from 'logic/users/User';
import CombinedProvider from 'injection/CombinedProvider';

import ReadOnlyWarning from './ReadOnlyWarning';
import SettingsSection from './SettingsSection';
import PasswordSection from './PasswordSection';
import ProfileSection from './ProfileSection';
import RolesSection from './RolesSection';

import MainDetailsGrid from '../UserDetails/MainDetailsGrid';
import SectionComponent from '../SectionComponent';

const { CurrentUserStore } = CombinedProvider.get('CurrentUser');

type Props = {
  user: ?User,
};

const _updateUser = (data, currentUser, user) => {
  return UsersActions.update(user.username, data).then(() => {
    UserNotification.success('User updated successfully.', 'Success');

    if (user.username === currentUser?.username) {
      CurrentUserStore.reload();
    }
  }, () => {
    UserNotification.error('Could not update the user. Please check your logs for more information.', 'Updating user failed');
  });
};

const UserEdit = ({ user }: Props) => {
  const currentUser = useContext(CurrentUserContext);

  if (!user) {
    return <Spinner />;
  }

  if (user.readOnly) {
    return <ReadOnlyWarning fullName={user.fullName} />;
  }

  return (
    <>
      <MainDetailsGrid>
        <div>
          <ProfileSection user={user}
                          onSubmit={(data) => _updateUser(data, currentUser, user)} />
          <SettingsSection user={user}
                           onSubmit={(data) => _updateUser(data, currentUser, user)} />
          <PasswordSection user={user} />
        </div>
        <div>
          <SectionComponent title="Teams">Children</SectionComponent>
          <RolesSection user={user}
                        onSubmit={(data) => _updateUser(data, currentUser, user)} />
        </div>
      </MainDetailsGrid>
    </>
  );
};

export default UserEdit;
