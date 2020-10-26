// @flow strict
import * as React from 'react';
import { useContext } from 'react';

import UsersDomain from 'domainActions/users/UsersDomain';
import CurrentUserContext from 'contexts/CurrentUserContext';
import { Spinner, IfPermitted } from 'components/common';
import User from 'logic/users/User';
import CombinedProvider from 'injection/CombinedProvider';

import ReadOnlyWarning from './ReadOnlyWarning';
import SettingsSection from './SettingsSection';
import PasswordSection from './PasswordSection';
import ProfileSection from './ProfileSection';
import PreferencesSection from './PreferencesSection';
import RolesSection from './RolesSection';
import TeamsSection from './TeamsSection';

import SectionGrid from '../../common/Section/SectionGrid';

const { CurrentUserStore } = CombinedProvider.get('CurrentUser');

type Props = {
  user: ?User,
};

const _updateUser = (data, currentUser, userId) => {
  return UsersDomain.update(userId, data).then(() => {
    if (userId === currentUser?.id) {
      CurrentUserStore.reload();
    }
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
    <SectionGrid>
      <IfPermitted permissions={`users:edit:${user.username}`}>
        <div>
          <ProfileSection user={user}
                          onSubmit={(data) => _updateUser(data, currentUser, user.id)} />
          <IfPermitted permissions="*">
            <SettingsSection user={user}
                             onSubmit={(data) => _updateUser(data, currentUser, user.id)} />
          </IfPermitted>
          <IfPermitted permissions={`users:passwordchange:${user.username}`}>
            <PasswordSection user={user} />
          </IfPermitted>
          <PreferencesSection user={user} />
        </div>
        <div>
          <IfPermitted permissions="users:rolesedit">
            <RolesSection user={user}
                          onSubmit={(data) => _updateUser(data, currentUser, user.id)} />
          </IfPermitted>
          <IfPermitted permissions="teams:edit">
            <TeamsSection user={user}
                          onSubmit={(data) => _updateUser(data, currentUser, user.id)} />
          </IfPermitted>
        </div>
      </IfPermitted>
    </SectionGrid>

  );
};

export default UserEdit;
