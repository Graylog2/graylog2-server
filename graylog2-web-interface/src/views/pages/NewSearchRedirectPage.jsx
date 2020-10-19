// @flow strict
import { useEffect } from 'react';

import history from 'util/History';
import Routes from 'routing/Routes';
import withParams from 'routing/withParams';

type Props = {
  params: {
    streamId?: string,
  },
};

const NewSearchRedirectPage = ({ params: { streamId } }: Props) => {
  useEffect(() => {
    if (streamId) {
      history.push(Routes.stream_search(streamId));
    } else {
      history.push(Routes.SEARCH);
    }
  }, [streamId]);

  return null;
};

export default withParams(NewSearchRedirectPage);
