import React from 'react';
import styled, { css } from 'styled-components';

import SectionGrid from 'components/common/Section/SectionGrid';
import { ButtonGroup } from 'components/bootstrap';
import SectionComponent from 'components/common/Section/SectionComponent';
import ContentStreamNews from 'components/content-stream/ContentStreamNews';
import ContentStreamNewsFooter from 'components/content-stream/news/ContentStreamNewsFooter';

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

const ContentStreamSection = () => (
  <SectionGrid $columns="2fr 1fr">
    <StyledNewsSectionComponent title="News">
      <ButtonGroup />
      <ContentStreamNews />
      <ContentStreamNewsFooter />
    </StyledNewsSectionComponent>
    <StyledReleaseSectionComponent title="Release">
      Release
    </StyledReleaseSectionComponent>
  </SectionGrid>
);

export default ContentStreamSection;
