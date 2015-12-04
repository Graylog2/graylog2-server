import React from 'react';
import numeral from 'numeral';
import moment from 'moment';

const DeflectorConfigSummary = React.createClass({
  propTypes: {
    config: React.PropTypes.object.isRequired,
  },
  render() {
    const { config } = this.props;
    switch (config.type) {
      case 'count':
        return (
          <strong>Your current configuration is {numeral(config.max_docs_per_index).format('0,0')} documents per index
            and a maximum number of {config.max_number_of_indices} indices.</strong>
        );
      case 'time':
        return (
          <strong>Your current configuration rotates the indices every {moment.duration(config.max_time_per_index).humanize()}
            and keeps a maximum number of {config.max_number_of_indices} indices.</strong>
        );
      case 'size':
        return (
          <strong>Your current configuration is {numeral(config.max_size_per_index).format('0,0')} bytes per index and a
            maximum number of {config.max_number_of_indices} indices.</strong>
        );
    }
  },
});

export default DeflectorConfigSummary;
