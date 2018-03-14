import React from 'react';
import { Link } from 'react-router';

import Routes from 'routing/Routes';

class StreamLink extends React.Component {
  render() {
    const stream = this.props.stream;
    const route = Routes.stream_search(stream.id);
    return <Link to={route}>{stream.title}</Link>;
  }
}

export default StreamLink;
