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
import isEmpty from 'lodash/isEmpty';

import Carousel from 'components/common/carousel/Carousel';
import useContentStream from 'components/content-stream/hook/useContentStream';
import { Spinner } from 'components/common';
import ContentStreamNewsItem from 'components/content-stream/news/ContentStreamNewsItem';

type Props = {
  rssUrl: string,
}

const ContentStreamNews = ({ rssUrl }: Props) => {
  const { feedList, isLoadingFeed } = useContentStream(rssUrl);

  if (isLoadingFeed && !isEmpty(feedList)) {
    return <Spinner />;
  }

  return (
    <Carousel>
      {feedList?.map((feed) => <ContentStreamNewsItem key={feed?.guid['#text'] || feed?.title} feed={feed} />)}
    </Carousel>
  );
};

export default ContentStreamNews;
