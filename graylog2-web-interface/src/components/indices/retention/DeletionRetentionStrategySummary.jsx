import React from 'react';

import style from '!style!css!../IndicesConfiguration.css';

const DeletionRetentionStrategySummary = React.createClass({
  propTypes: {
    config: React.PropTypes.object.isRequired,
  },

  render() {
    return (
      <div>
        <dl className={style.deflist}>
          <dt>Index rotation strategy:</dt>
          <dd>Delete</dd>
          <dt>Max number of indices:</dt>
          <dd>{this.props.config.max_number_of_indices}</dd>
        </dl>
      </div>
    );
  },
});

export default DeletionRetentionStrategySummary;
