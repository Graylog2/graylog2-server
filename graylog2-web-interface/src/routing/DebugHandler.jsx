import React from 'react';

const DebugHandler = React.createClass({
  render() {
    return (
      <div>
        <h1>DebugHandler</h1>
        {this.props.location.pathname}
      </div>
    );
  },
});

export default DebugHandler;
