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
      {feedList?.map((feed) => <ContentStreamNewsItem key={feed['post-id']} feed={feed} />)}
    </Carousel>
  );
};

export default ContentStreamNews;
