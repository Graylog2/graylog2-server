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
import DOMPurify from 'dompurify';

import { Carousel, Timestamp } from 'components/common';
import { Panel } from 'components/bootstrap';
import type { FeedITem } from 'components/content-stream/hook/useContentStream';

type Props = {
  feed: FeedITem
}

const StyledImage = styled.img`
  max-width: 100%;
  width: 100%;
  object-fit: contain;
  border-radius: 4px;
`;
const StyledPanelBody = styled(Panel.Body)(({ theme }: { theme: DefaultTheme }) => css`
  flex-grow: 1;
  background-color: ${theme.colors.table.backgroundAlt};

  > a {
    font-weight: bold;
  }
`);
const StyledPanelFooter = styled(Panel.Footer)(({ theme }: { theme: DefaultTheme }) => css`
  background-color: ${theme.colors.table.backgroundAlt};
  border-radius: 0 0 4px 4px;
`);
const StyledPanel = styled(Panel)`
  display: flex;
  flex-direction: column;
  height: 100%;
  border: none;
  border-radius: 4px;
`;
const _sanitizeText = (text) => DOMPurify.sanitize(text);

const ContentStreamNewsItem = ({ feed }: Props) => (
  <Carousel.Slide>
    <StyledPanel>
      <StyledImage src="https://placehold.co/600x400" alt="test" />
      <StyledPanelBody>
        {/* eslint-disable-next-line react/no-danger */}
        <a href={feed.link} target="_blank" rel="noreferrer"><span dangerouslySetInnerHTML={{ __html: _sanitizeText(feed.title) }} />
        </a>
      </StyledPanelBody>
      <StyledPanelFooter>
        <Timestamp dateTime={_sanitizeText(feed.pubDate)} format="shortReadable" />
      </StyledPanelFooter>
    </StyledPanel>
  </Carousel.Slide>
);

export default ContentStreamNewsItem;
