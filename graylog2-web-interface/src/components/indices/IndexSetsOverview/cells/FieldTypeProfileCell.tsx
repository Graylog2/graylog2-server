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
import * as React from 'react';

import Routes from 'routing/Routes';
import { Link } from 'components/common';
import useProfile from 'components/indices/IndexSetFieldTypeProfiles/hooks/useProfile';
import usePermissions from 'hooks/usePermissions';

type Props = {
  profileId: string | null | undefined;
};

const FieldTypeProfileCell = ({ profileId }: Props) => {
  const { isPermitted } = usePermissions();
  const canReadProfile = !!profileId && isPermitted(`mappingprofiles:read:${profileId}`);
  const {
    data: { name: profileName },
    isFetching,
  } = useProfile(canReadProfile ? profileId : undefined);

  if (!profileId) {
    return <i>Not set</i>;
  }

  if (!canReadProfile) {
    return <span>{profileId}</span>;
  }

  if (isFetching) {
    return null;
  }

  return (
    <Link to={Routes.SYSTEM.INDICES.FIELD_TYPE_PROFILES.edit(profileId)} target="_blank">
      {profileName ?? profileId}
    </Link>
  );
};

export default FieldTypeProfileCell;
