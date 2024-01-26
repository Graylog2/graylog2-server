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
import React from 'react';

import { Button, ButtonToolbar, MenuItem } from 'components/bootstrap';
import Routes from 'routing/Routes';
import { LinkContainer } from 'components/common/router';
import MoreActions from 'components/common/EntityDataTable/MoreActions';
import useSelectedEntities from 'components/common/EntityDataTable/hooks/useSelectedEntities';
import useProfileMutations from 'components/indices/IndexSetFieldTypeProfiles/hooks/useProfileMutations';

const ProfileActions = ({ profileId, profileName }: { profileId: string, profileName: string }) => {
  const { deselectEntity } = useSelectedEntities();
  const { deleteProfile } = useProfileMutations();

  const onDelete = () => {
    // eslint-disable-next-line no-alert
    if (window.confirm(`You are about to delete profile: "${profileName}". Are you sure?`)) {
      deleteProfile(profileId).then(() => {
        deselectEntity(profileId);
      });
    }
  };

  return (
    <ButtonToolbar>
      <LinkContainer to={Routes.SYSTEM.INDICES.FIELD_TYPE_PROFILES.edit(profileId)}>
        <Button bsSize="xs">
          Edit
        </Button>
      </LinkContainer>
      <MoreActions>
        <MenuItem onSelect={onDelete}>
          Delete
        </MenuItem>
      </MoreActions>
    </ButtonToolbar>
  );
};

export default ProfileActions;
