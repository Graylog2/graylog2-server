/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import PropTypes from 'prop-types';
import React from 'react';

import { Input } from 'components/bootstrap';
import ISODurationUtils from 'util/ISODurationUtils';

/**
 * Displays an `Input` component for ISO8601 durations.
 */
class ISODurationInput extends React.Component {
  static propTypes = {
    /** Input id */
    id: PropTypes.string.isRequired,
    /** Value to show in the Input. */
    duration: PropTypes.string.isRequired,
    /** Callback that will receive the validated duration when the input changes. */
    update: PropTypes.func.isRequired,
    /** Input label. */
    label: PropTypes.string,
    /** Input help text. */
    help: PropTypes.string,
    /**
     * Function that validates the duration. It receives the duration in milliseconds and the duration itself as
     * arguments, and must return `true` if the duration is valid or `false` if not. Only valid durations are propagated
     * after the input changes.
     */
    validator: PropTypes.func,
    /** Text to display when duration is invalid. */
    errorText: PropTypes.string,
    /** Specify that the Input should have input focus when the page loads. */
    autoFocus: PropTypes.bool,
    /** Specify that the Input is required to submit the form. */
    required: PropTypes.bool,
  };

  static defaultProps = {
    label: 'Duration',
    help: 'as ISO8601 Duration',
    validator: () => true,
    errorText: 'invalid',
    autoFocus: false,
    required: false,
  };

  state = {
    duration: this.props.duration,
  };

  _onUpdate = () => {
    let duration = this.isoDuration.getValue().toUpperCase();

    if (!duration.startsWith('P')) {
      duration = `P${duration}`;
    }

    this.setState({ duration: duration });

    if (ISODurationUtils.isValidDuration(duration, this.props.validator)) {
      // Only propagate state if the config is valid.
      this.props.update(duration);
    }
  };

  render() {
    return (
      <Input id={this.props.id}
             type="text"
             ref={(isoDuration) => { this.isoDuration = isoDuration; }}
             label={this.props.label}
             onChange={this._onUpdate}
             value={this.state.duration}
             help={this.props.help}
             addonAfter={ISODurationUtils.humanizeDuration(this.state.duration, this.props.validator, this.props.errorText)}
             bsStyle={ISODurationUtils.durationStyle(this.state.duration, this.props.validator)}
             autoFocus={this.props.autoFocus}
             required={this.props.required} />
    );
  }
}

export default ISODurationInput;
