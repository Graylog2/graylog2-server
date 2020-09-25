// @flow strict
import * as React from 'react';
import { useContext } from 'react';

import StreamsContext from 'contexts/StreamsContext';
import UserHasNoStreamAccess from 'pages/UserHasNoStreamAccess';

type Props = {
  children: React.Node,
};

export default ({ children }: Props) => {
  const streams = useContext(StreamsContext);

  return (streams && streams.length > 0 ? children : <UserHasNoStreamAccess />);
};
