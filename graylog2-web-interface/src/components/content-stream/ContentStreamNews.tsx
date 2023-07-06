import React from 'react';
import isEmpty from 'lodash/isEmpty';

import Carousel from 'components/common/carousel/Carousel';
import useContentStream from 'components/content-stream/hook/useContentStream';
import { Spinner } from 'components/common';
import ContentStreamNewsItem from 'components/content-stream/news/ContentStreamNewsItem';

const ContentStreamNews = () => {
  const { newsList, isLoadingFeed } = useContentStream();

  if (isLoadingFeed && !isEmpty(newsList)) {
    return <Spinner />;
  }

  return (
    <Carousel>
      {newsList?.map((news) => <ContentStreamNewsItem feed={news} />)}
    </Carousel>
  );
};

export default ContentStreamNews;
