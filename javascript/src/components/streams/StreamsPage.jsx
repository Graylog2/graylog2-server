import React from 'react';
import StreamComponent from './StreamComponent';

const StreamsPage = React.createClass({
  render() {
    return <StreamComponent permissions={['*']} username={'admin'}/>;
  }
});

export default StreamsPage;
