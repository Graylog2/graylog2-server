import PropTypes from 'prop-types';
import React from 'react';
import { Label } from 'react-bootstrap';

class StreamStateBadge extends React.Component {
  static propTypes = {
    stream: PropTypes.object.isRequired,
  };

  render() {
    if (this.props.stream.is_default) {
      return <Label bsStyle="primary">Default</Label>;
    }

    if (!this.props.stream.disabled) {
      return null;
    }

    return <Label bsStyle="warning">Stopped</Label>;
  }
}

export default StreamStateBadge;
