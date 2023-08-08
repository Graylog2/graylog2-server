/*
import { useQuery } from '@tanstack/react-query';

import { ContentStream } from '@graylog/server-api';
import UserNotification from 'preflight/util/UserNotification';
import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';

export const CONTENT_STREAM_SETTINGS_KEY = 'CONTENT_STREAM_SETTINGS';

const useContentStream = () => {
  const { data, isLoading } = useQuery([CONTENT_STREAM_SETTINGS], () => {
  }, {
    onError: (errorThrown) => {
      UserNotification.error(`Loading news feed failed with status: ${errorThrown}`,
        'Could not load news feed');
    },
    initialData: [],
  });

  return {
    newsList: data,
    isLoadingFeed: isLoading,
  };
};

export default useContentStream;
*/
