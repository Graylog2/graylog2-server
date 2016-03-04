import React from 'react';
import { Input } from 'react-bootstrap';
import moment from 'moment';

const ISODurationInput = React.createClass({
  propTypes: {
    duration: React.PropTypes.string.isRequired,
    update: React.PropTypes.func.isRequired,
    label: React.PropTypes.string,
    help: React.PropTypes.string,
    validator: React.PropTypes.func,
    errorText: React.PropTypes.string,
    autoFocus: React.PropTypes.bool,
    required: React.PropTypes.bool,
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

  _isValidDuration(duration) {
    return this.props.validator(moment.duration(duration).asMilliseconds());
  },

  _validationState() {
    return this._isValidDuration(this.state.duration) ? null : 'error';
  },

  _formatDuration() {
    return this._isValidDuration(this.state.duration) ? moment.duration(this.state.duration).humanize() : this.props.errorText;
  },

  _onUpdate() {
    let duration = this.refs.isoDuration.getValue().toUpperCase();

    if (!duration.startsWith('P')) {
      duration = `P${duration}`;
    }

    this.setState({duration: duration});

    if (this._isValidDuration(duration)) {
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
             addonAfter={this._formatDuration()}
             bsStyle={this._validationState()}
             autofocus={this.props.autoFocus}
             required={this.props.required} />
    );
  },
});

export default ISODurationInput;
