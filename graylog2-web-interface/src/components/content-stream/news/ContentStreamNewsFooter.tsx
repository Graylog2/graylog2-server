import React from 'react';
import type { DefaultTheme } from 'styled-components';
import styled, { css } from 'styled-components';

import ContentStreamNewsContentActions from 'components/content-stream/news/ContentStreamNewsContentActions';

const StyledDiv = styled.div(({ theme }: { theme: DefaultTheme }) => css`
  display: grid;
  grid-template-columns: 1fr 1fr;
  grid-template-rows: 1fr;
  grid-auto-flow: row;
  margin-top: ${theme.spacings.sm};
`);
const StyledActionDiv = styled.div`
  display: flex;
  align-items: center;
  justify-content: right;
`;

const StyledReadMoreDiv = styled.div`
  justify-content: center;
  display: flex;
  flex-direction: column;
`;
const ContentStreamNewsFooter = () => (
  <StyledDiv>
    <StyledReadMoreDiv>
      <a href="https://www.graylog.org/blog/" target="_blank" rel="noreferrer">
        Read more
      </a>
    </StyledReadMoreDiv>
    <StyledActionDiv><ContentStreamNewsContentActions /></StyledActionDiv>
  </StyledDiv>
);

export default ContentStreamNewsFooter;
