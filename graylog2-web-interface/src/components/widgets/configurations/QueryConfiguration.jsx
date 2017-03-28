import React, { PropTypes } from 'react';
import { Input } from 'components/bootstrap';

const QueryConfiguration = React.createClass({
  propTypes: {
    config: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired,
  },
  render() {
    return (
      <Input type="text"
             key="query"
             id="query"
             name="query"
             label="Search query"
             defaultValue={this.props.config.query}
             onChange={this.props.onChange}
             help="Search query that will be executed to get the widget value." />
    );
  },
});

export default QueryConfiguration;
