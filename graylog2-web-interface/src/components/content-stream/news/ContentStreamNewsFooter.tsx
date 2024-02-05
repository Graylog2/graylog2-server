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
import type { DefaultTheme } from 'styled-components';
import styled, { css } from 'styled-components';

import ContentStreamNewsContentActions from 'components/content-stream/news/ContentStreamNewsContentActions';
import { ExternalLink } from 'components/common';

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
      <ExternalLink href="https://www.graylog.org/blog/">
        Read more
      </ExternalLink>
    </StyledReadMoreDiv>
    <StyledActionDiv><ContentStreamNewsContentActions /></StyledActionDiv>
  </StyledDiv>
);

export default ContentStreamNewsFooter;
