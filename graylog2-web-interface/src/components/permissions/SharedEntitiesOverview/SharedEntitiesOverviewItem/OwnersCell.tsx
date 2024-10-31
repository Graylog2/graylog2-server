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

import { isPermitted } from 'util/PermissionsMixin';
import type Grantee from 'logic/permissions/Grantee';
import { Link } from 'components/common/router';
import { defaultCompare } from 'logic/DefaultCompare';
import useCurrentUser from 'hooks/useCurrentUser';
import type { GranteesList } from 'logic/permissions/EntityShareState';
import useShowRouteFromGRN from 'routing/hooks/useShowRouteFromGRN';

type Props = {
  owners: GranteesList,
};

const TitleWithLink = ({ title, entityId }: { title: string, entityId: string }) => {
  const entityRoute = useShowRouteFromGRN(entityId);

  return <Link to={entityRoute}>{title}</Link>;
};

const assertUnreachable = (type: 'error'): never => {
  throw new Error(`Owner of entity has not supported type: ${type}`);
};

type OwnerTitleProps = {
  owner: Grantee
}

const OwnerTitle = ({ owner: { type, id, title } }: OwnerTitleProps) => {
  const currentUser = useCurrentUser();

  switch (type) {
    case 'user':
      if (!isPermitted(currentUser.permissions, 'users:list')) return <span>{title}</span>;

      return <TitleWithLink entityId={id} title={title} />;
    case 'team':
      if (!isPermitted(currentUser.permissions, 'teams:list')) return <span>{title}</span>;

      return <TitleWithLink entityId={id} title={title} />;
    case 'global':
      return <span>Everyone</span>;
    default:
      return assertUnreachable(type);
  }
};

const OwnersCell = ({ owners }: Props) => {
  const sortedOwners = owners.sort((o1, o2) => defaultCompare(o1.type, o2.type) || defaultCompare(o1.title, o2.title));

  return (
    <td className="limited">
      {sortedOwners.map((owner, index) => {
        const isLast = index >= owners.size - 1;

        return (
          <React.Fragment key={owner.id}>
            <OwnerTitle owner={owner} />
            {!isLast && ', '}
          </React.Fragment>
        );
      }).toArray()}
    </td>
  );
};

export default OwnersCell;
