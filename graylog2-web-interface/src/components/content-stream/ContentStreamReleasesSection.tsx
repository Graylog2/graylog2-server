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
import isEmpty from 'lodash/isEmpty';
import DOMPurify from 'dompurify';

import { ListGroup, ListGroupItem, Label } from 'components/bootstrap';
import { RelativeTime, Spinner } from 'components/common';
import useContentStream from 'components/content-stream/hook/useContentStream';

type Props = {
  rssUrl: string,
};

const StyledListGroupItem = styled(ListGroupItem)`
  display: flex;
  gap: 16px;
  align-items: flex-start;
`;
const LastOpenedTime = styled.i(({ theme }: { theme: DefaultTheme }) => css`
  color: ${theme.colors.gray[60]};
`);
export const StyledLabel = styled(Label)`
  cursor: default;
  width: 110px;
  display: block;
`;

const _sanitizeText = (text) => DOMPurify.sanitize(text);

const ContentStreamReleasesSection = ({ rssUrl }: Props) => {
  const { feedList, isLoadingFeed } = useContentStream(rssUrl);

  if (isLoadingFeed && !isEmpty(feedList)) {
    return <Spinner />;
  }

  return (
    <ListGroup>
      {feedList.map((feed) => (
        <StyledListGroupItem key={feed?.guid['#text'] || feed?.title}>
          <a href={feed?.link} target="_blank" rel="noreferrer">
            {/* eslint-disable-next-line react/no-danger */}
            <span dangerouslySetInnerHTML={{ __html: _sanitizeText(feed.title) }} />
          </a>
          {feed?.pubDate ? <LastOpenedTime><RelativeTime dateTime={feed.pubDate} /></LastOpenedTime> : null}
        </StyledListGroupItem>
      ))}
    </ListGroup>
  );
};

export default ContentStreamReleasesSection;
