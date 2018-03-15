import PropTypes from 'prop-types';
import React from 'react';

class DeletionRetentionStrategySummary extends React.Component {
  static propTypes = {
    config: PropTypes.object.isRequired,
  };

  render() {
    return (
      <div>
        <dl>
          <dt>Index retention strategy:</dt>
          <dd>Delete</dd>
          <dt>Max number of indices:</dt>
          <dd>{this.props.config.max_number_of_indices}</dd>
        </dl>
      </div>
    );
  }
}

export default DeletionRetentionStrategySummary;
