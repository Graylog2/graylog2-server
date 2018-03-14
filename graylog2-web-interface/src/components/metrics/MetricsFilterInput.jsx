import React from 'react';
import PropTypes from 'prop-types';
import { FormControl } from 'react-bootstrap';

class MetricsFilterInput extends React.Component {
  static propTypes = {
    filter: PropTypes.string.isRequired,
    onChange: PropTypes.func.isRequired,
  };

  handleChange = (event) => {
    this.props.onChange(event.target.value);
  };

  render() {
    const { filter } = this.props;
    return (
      <FormControl type="text"
                   className="metrics-filter"
                   bsSize="large"
                   placeholder="Type a metric name to filter&hellip;"
                   value={filter}
                   onChange={this.handleChange} />
    );
  }
}

export default MetricsFilterInput;
