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
import styled from 'styled-components';

import PageHeader from 'components/common/PageHeader';
import SectionComponent from 'components/common/Section/SectionComponent';

import LastOpenList from './LastOpenList';
import FavoriteItemsList from './FavoriteItemsList';
import RecentActivityList from './RecentActivityList';

import SectionGrid from '../common/Section/SectionGrid';
import useCurrentUser from '../../hooks/useCurrentUser';

const StyledSectionComponent = styled(SectionComponent)`
  flex-grow: 1;
`;

const Welcome = () => {
  const { permissions } = useCurrentUser();
  const isAdmin = permissions.includes('*');

  return (
    <>
      <PageHeader title="Welcome to Graylog!">
        <span>
          This is your personal start page, allowing easy access to the content most relevant for you.
        </span>
      </PageHeader>
      <SectionGrid>
        <StyledSectionComponent title="Last Opened">
          <p className="description">Overview of recently visited saved searches and dashboards.</p>
          <LastOpenList />
        </StyledSectionComponent>
        <StyledSectionComponent title="Favorite Items">
          <p className="description">Overview of your favorite saved searches and dashboards.</p>
          <FavoriteItemsList />
        </StyledSectionComponent>
      </SectionGrid>
      <StyledSectionComponent title="Recent Activity">
        <p className="description">
          {isAdmin
            ? 'This list includes all actions Graylog users performed, like creating or sharing an entity.'
            : 'Overview of actions you made with entities or somebody else made with entities which relates to you, like creating or sharing an entity.'}
        </p>
        <RecentActivityList />
      </StyledSectionComponent>
    </>
  );
};

export default Welcome;
