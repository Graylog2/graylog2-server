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

import { Spinner } from 'components/common';
import type Grantee from 'logic/permissions/Grantee';
import {
  GranteeInfo,
  GranteeListItemContainer,
  GranteeListItemTitle,
  StyledGranteeIcon,
  StyledGranteeList,
} from 'components/permissions/CommonStyledComponents';

import useEntityGrantees from './useEntityGrantees';
import { capabilityTitle, sortGranteeShares } from './shareCounts';

type Props = {
  grn: string;
  // Names the entity type in the text for entities without grantees, e.g. "stream".
  entityTypeTitle: string;
};

const SharedWithOthersSection = ({ grn, entityTypeTitle }: Props) => {
  const { data, isInitialLoading, isError } = useEntityGrantees(grn);

  if (isInitialLoading) {
    return <Spinner />;
  }

  if (isError) {
    return <p>Could not load sharing information.</p>;
  }

  if (!data?.length) {
    return <p>This {entityTypeTitle} is not shared with others.</p>;
  }

  return (
    <StyledGranteeList>
      {sortGranteeShares(data).map(({ grantee, capability }) => (
        <GranteeListItemContainer key={grantee.id} $currentState={null}>
          <GranteeInfo title={grantee.title}>
            <StyledGranteeIcon type={grantee.type as Grantee['type']} />
            <GranteeListItemTitle>{grantee.title}</GranteeListItemTitle>
          </GranteeInfo>
          <GranteeInfo>{capabilityTitle(capability)}</GranteeInfo>
        </GranteeListItemContainer>
      ))}
    </StyledGranteeList>
  );
};

export default SharedWithOthersSection;
