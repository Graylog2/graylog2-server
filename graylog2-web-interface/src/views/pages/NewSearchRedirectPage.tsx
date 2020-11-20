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
