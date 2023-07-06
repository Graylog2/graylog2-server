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
import styled, { css } from 'styled-components';

import SectionGrid from 'components/common/Section/SectionGrid';
import { ButtonGroup } from 'components/bootstrap';
import SectionComponent from 'components/common/Section/SectionComponent';
import ContentStreamNews from 'components/content-stream/ContentStreamNews';
import ContentStreamNewsFooter from 'components/content-stream/news/ContentStreamNewsFooter';
import AppConfig from 'util/AppConfig';

const StyledNewsSectionComponent = styled(SectionComponent)(({ theme }) => css`
  overflow: hidden;
  flex-grow: 3;
  @media (max-width: ${theme.breakpoints.max.md}) {
    flex-grow: 1;
  }
`);
const StyledReleaseSectionComponent = styled(SectionComponent)`
  flex-grow: 1;
`;

const ContentStreamSection = () => {
  const { rss_url } = AppConfig.contentStream();

  return (
    rss_url && (
      <SectionGrid $columns="2fr 1fr">
        <StyledNewsSectionComponent title="News">
          <ButtonGroup />
          <ContentStreamNews rssUrl={rss_url} />
          <ContentStreamNewsFooter />
        </StyledNewsSectionComponent>
        <StyledReleaseSectionComponent title="Release">
          Release
        </StyledReleaseSectionComponent>
      </SectionGrid>
    )
  );
};

export default ContentStreamSection;
