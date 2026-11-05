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
import { useTranslation } from 'react-i18next';

import { Button, ButtonToolbar, DeleteMenuItem } from 'components/bootstrap';
import Routes from 'routing/Routes';
import { LinkContainer } from 'components/common';
import { MoreActions } from 'components/common/EntityDataTable';
import useSelectedEntities from 'components/common/EntityDataTable/hooks/useSelectedEntities';
import useProfileMutations from 'components/indices/IndexSetFieldTypeProfiles/hooks/useProfileMutations';

const ProfileActions = ({ profileId, profileName }: { profileId: string; profileName: string }) => {
  const { t } = useTranslation('indexsets');
  const { deselectEntity } = useSelectedEntities();
  const { deleteProfile } = useProfileMutations();

  const onDelete = () => {
    // eslint-disable-next-line no-alert
    if (window.confirm(t('profiles.confirm_delete', { profileName }))) {
      deleteProfile(profileId).then(() => {
        deselectEntity(profileId);
      });
    }
  };

  return (
    <ButtonToolbar>
      <LinkContainer to={Routes.SYSTEM.INDICES.FIELD_TYPE_PROFILES.edit(profileId)}>
        <Button bsSize="xs">{t('common:actions.edit')}</Button>
      </LinkContainer>
      <MoreActions>
        <DeleteMenuItem onSelect={onDelete} />
      </MoreActions>
    </ButtonToolbar>
  );
};

export default ProfileActions;
