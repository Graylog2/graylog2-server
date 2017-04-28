import React, { PropTypes } from 'react';

import { Input } from 'components/bootstrap';

const RandomAdapterFieldSet = React.createClass({
  propTypes: {
    config: PropTypes.object.isRequired,
// eslint-disable-next-line react/no-unused-prop-types
    updateConfig: PropTypes.func.isRequired,
    handleFormEvent: PropTypes.func.isRequired,
  },

  render() {
    const config = this.props.config;

    return (<fieldset>
      <Input type="text"
             id="lower_bound"
             name="lower_bound"
             label="Minimum value"
             autoFocus
             required
             onChange={this.props.handleFormEvent}
             help="The minimum integer this data adapter generates."
             value={config.lower_bound}
             labelClassName="col-sm-3"
             wrapperClassName="col-sm-9" />
      <Input type="text"
             id="upper_bound"
             name="upper_bound"
             label="Upper bound"
             autoFocus
             required
             onChange={this.props.handleFormEvent}
             help="All generated integers are lower than this value."
             value={config.upper_bound}
             labelClassName="col-sm-3"
             wrapperClassName="col-sm-9" />
    </fieldset>);
  },
});

export default RandomAdapterFieldSet;
