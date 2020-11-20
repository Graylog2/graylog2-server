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
import { useContext } from 'react';

import { Link } from 'components/graylog/router';
import { defaultCompare } from 'views/logic/DefaultCompare';
import { isPermitted } from 'util/PermissionsMixin';
import CurrentUserContext from 'contexts/CurrentUserContext';
import type { GranteesList } from 'logic/permissions/EntityShareState';
import { getShowRouteFromGRN } from 'logic/permissions/GRN';
import { UserJSON } from 'logic/users/User';

type Props = {
  owners: GranteesList,
};

const TitleWithLink = ({ to, title }: { to: string, title: string }) => <Link to={to}>{title}</Link>;

const _getOwnerTitle = ({ type, id, title }, userPermissions) => {
  const link = getShowRouteFromGRN(id);

  switch (type) {
    case 'user':
      if (!isPermitted(userPermissions, 'users:list')) return title;

      return <TitleWithLink to={link} title={title} />;
    case 'team':
      if (!isPermitted(userPermissions, 'teams:list')) return title;

      return <TitleWithLink to={link} title={title} />;
    default:
      throw new Error(`Owner of entity has not supported type: ${type}`);
  }
};

const OwnersCell = ({ owners }: Props) => {
  const currentUser = useContext<UserJSON | undefined>(CurrentUserContext);
  const sortedOwners = owners.sort((o1, o2) => defaultCompare(o1.type, o2.type) || defaultCompare(o1.title, o2.title));

  return (
    <td className="limited">
      {sortedOwners.map((owner, index) => {
        const title = _getOwnerTitle(owner, currentUser?.permissions);
        const isLast = index >= owners.size - 1;

        return (
          <React.Fragment key={owner.id}>
            {title}
            {!isLast && ', '}
          </React.Fragment>
        );
      })}
    </td>
  );
};

export default OwnersCell;
