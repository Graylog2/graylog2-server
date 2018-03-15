import React from 'react';

class DebugHandler extends React.Component {
  render() {
    return (
      <div>
        <h1>DebugHandler</h1>
        {this.props.location.pathname}
      </div>
    );
  }
}

export default DebugHandler;
