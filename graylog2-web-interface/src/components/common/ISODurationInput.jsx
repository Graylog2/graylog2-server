import PropTypes from 'prop-types';
import React from 'react';
import { Input } from 'components/bootstrap';
import ISODurationUtils from 'util/ISODurationUtils';

const ISODurationInput = React.createClass({
  propTypes: {
    duration: PropTypes.string.isRequired,
    update: PropTypes.func.isRequired,
    label: PropTypes.string,
    help: PropTypes.string,
    validator: PropTypes.func,
    errorText: PropTypes.string,
    autoFocus: PropTypes.bool,
    required: PropTypes.bool,
  },

  getDefaultProps() {
    return {
      label: 'Duration',
      help: 'as ISO8601 Duration',
      validator: () => true,
      errorText: 'invalid',
      autoFocus: false,
      required: false,
    };
  },

  getInitialState() {
    return {
      duration: this.props.duration,
    };
  },

  _onUpdate() {
    let duration = this.refs.isoDuration.getValue().toUpperCase();

    if (!duration.startsWith('P')) {
      duration = `P${duration}`;
    }

    this.setState({ duration: duration });

    if (ISODurationUtils.isValidDuration(duration, this.props.validator)) {
      // Only propagate state if the config is valid.
      this.props.update(duration);
    }
  },

  render() {
    return (
      <Input type="text"
             ref="isoDuration"
             label={this.props.label}
             onChange={this._onUpdate}
             value={this.state.duration}
             help={this.props.help}
             addonAfter={ISODurationUtils.humanizeDuration(this.state.duration, this.props.validator, this.props.errorText)}
             bsStyle={ISODurationUtils.durationStyle(this.state.duration, this.props.validator)}
             autofocus={this.props.autoFocus}
             required={this.props.required} />
    );
  },
});

export default ISODurationInput;
