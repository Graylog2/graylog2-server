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
// @flow strict
import * as React from 'react';
import { useState, useCallback } from 'react';

import AuthzRolesDomain from 'domainActions/roles/AuthzRolesDomain';
import User from 'logic/users/User';
import PaginatedItemOverview, { type PaginatedListType } from 'components/common/PaginatedItemOverview';
import SectionComponent from 'components/common/Section/SectionComponent';

import RolesQueryHelp from '../RolesQueryHelp';

type Props = {
  user: User,
};

const RolesSection = ({ user: { username } }: Props) => {
  const [loading, setLoading] = useState(false);

  const _onLoad = useCallback((pagination, isSubscribed: boolean) => {
    setLoading(true);

    return AuthzRolesDomain.loadRolesForUser(username, pagination).then((paginatedRoles): PaginatedListType => {
      if (isSubscribed) {
        setLoading(false);
      }

      // $FlowFixMe Roles is a DescriptiveItem
      return paginatedRoles;
    });
  }, [username]);

  return (
    <SectionComponent title="Roles" showLoading={loading}>
      <PaginatedItemOverview noDataText="No selected roles have been found."
                             onLoad={_onLoad}
                             queryHelper={<RolesQueryHelp />} />
    </SectionComponent>
  );
};

export default RolesSection;
