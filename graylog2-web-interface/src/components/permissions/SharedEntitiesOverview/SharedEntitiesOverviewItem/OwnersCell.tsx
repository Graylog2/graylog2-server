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
import styled from 'styled-components';

import type Grantee from 'logic/permissions/Grantee';
import { Link } from 'components/common/router';
import { RestrictedAccessTooltip } from 'components/common';
import { defaultCompare } from 'logic/DefaultCompare';
import type { GranteesList } from 'logic/permissions/EntityShareState';
import useShowRouteFromGRN from 'routing/hooks/useShowRouteFromGRN';
import { isPermitted } from 'util/PermissionsMixin';
import useCurrentUser from 'hooks/useCurrentUser';
import { getValuesFromGRN } from 'logic/permissions/GRN';

type Props = {
  owners: GranteesList;
};

const OwnerList = styled.div`
  display: flex;
`;

const OwnerTitleWrapper = styled.span`
  display: flex;
  align-items: center;
`;

const TitleWithLink = ({ title, entityGrn }: { title: string; entityGrn: string }) => {
  const entityRoute = useShowRouteFromGRN(entityGrn);

  return <Link to={entityRoute}>{title}</Link>;
};

const assertUnreachable = (type: 'error'): never => {
  throw new Error(`Owner of entity has not supported type: ${type}`);
};

type OwnerTitleProps = {
  owner: Grantee;
};

const OwnerTitle = ({ owner: { type, id: grn, title } }: OwnerTitleProps) => {
  const currentUser = useCurrentUser();
  const { id: ownerId } = getValuesFromGRN(grn);

  switch (type) {
    case 'user':
      if (!isPermitted(currentUser.permissions, `users:edit:${ownerId}`))
        return (
          <OwnerTitleWrapper>
            {title} <RestrictedAccessTooltip entityName={type} capabilityName="view" />
          </OwnerTitleWrapper>
        );

      return <TitleWithLink title={title} entityGrn={grn} />;
    case 'team':
      if (!isPermitted(currentUser.permissions, `team:edit:${ownerId}`))
        return (
          <OwnerTitleWrapper>
            {title} <RestrictedAccessTooltip entityName={type} capabilityName="view" />
          </OwnerTitleWrapper>
        );

      return <TitleWithLink title={title} entityGrn={grn} />;
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
      <OwnerList>
        {sortedOwners
          .map((owner, index) => {
            const isLast = index >= owners.size - 1;

            return (
              <React.Fragment key={owner.id}>
                <OwnerTitle owner={owner} />
                {!isLast && <>, &nbsp;</>}
              </React.Fragment>
            );
          })
          .toArray()}
      </OwnerList>
    </td>
  );
};

export default OwnersCell;
