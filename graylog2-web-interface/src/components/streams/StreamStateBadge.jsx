import React from 'react';
import { Label } from 'react-bootstrap';

const StreamStateBadge = React.createClass({
  propTypes: {
    stream: React.PropTypes.object.isRequired,
  },
  render() {
    if (this.props.stream.is_default) {
      return <Label bsStyle="primary">Default</Label>;
    }

    if (!this.props.stream.disabled) {
      return null;
    }

    return <Label bsStyle="warning">Stopped</Label>;
  },
});

export default StreamStateBadge;
