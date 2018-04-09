import PropTypes from 'prop-types';
import React from 'react';

class ContentPackDependencyResolution extends React.Component {
  static propTypes = {
    contentPack: PropTypes.object.isRequired,
  };

  render() {
    return (<div>{this.props.contentPack.name}</div>);
  }
}

export default ContentPackDependencyResolution;
