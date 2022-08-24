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
import type { List } from 'immutable';

import { isPermitted } from 'util/PermissionsMixin';
import type Grantee from 'logic/permissions/Grantee';
import { Link } from 'components/common/router';
import { defaultCompare } from 'logic/DefaultCompare';
import useCurrentUser from 'hooks/useCurrentUser';
import type { GranteesList } from 'logic/permissions/EntityShareState';
import { getShowRouteFromGRN } from 'logic/permissions/GRN';

type Props = {
  owners: GranteesList,
};

const TitleWithLink = ({ to, title }: { to: string, title: string }) => <Link to={to}>{title}</Link>;

const assertUnreachable = (type: 'error'): never => {
  throw new Error(`Owner of entity has not supported type: ${type}`);
};

const _getOwnerTitle = ({ type, id, title }: Grantee, userPermissions: List<string>) => {
  switch (type) {
    case 'user':
      if (!isPermitted(userPermissions, 'users:list')) return title;

      return <TitleWithLink to={getShowRouteFromGRN(id)} title={title} />;
    case 'team':
      if (!isPermitted(userPermissions, 'teams:list')) return title;

      return <TitleWithLink to={getShowRouteFromGRN(id)} title={title} />;
    case 'global':
      return 'Everyone';
    default:
      return assertUnreachable(type);
  }
};

const OwnersCell = ({ owners }: Props) => {
  const currentUser = useCurrentUser();
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
