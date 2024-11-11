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
import React from 'react';

import { Input } from 'components/bootstrap';
import * as ISODurationUtils from 'util/ISODurationUtils';

/**
 * Displays an `Input` component for ISO8601 durations.
 */
type Props = {
  id: string,
  duration: string,
  update: (newDuration: string) => void,
  label: string,
  help: string,
  validator?: (newDuration: number) => boolean,
  errorText?: string,
  autoFocus?: boolean,
  required?: boolean,
  disabled?: boolean,
}
type State = {
  duration: string,
}

interface DurationInput {
  getValue(): string | boolean;
}

class ISODurationInput extends React.Component<Props, State> {
  private isoDuration: DurationInput;

  static defaultProps = {
    label: 'Duration',
    help: 'as ISO8601 Duration',
    validator: () => true,
    errorText: 'invalid',
    autoFocus: false,
    required: false,
    disabled: false,
  };

  constructor(props) {
    super(props);

    this.state = {
      duration: this.props.duration,
    };
  }

  _onUpdate = () => {
    let duration = this.isoDuration.getValue().toString().toUpperCase();

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
             required={this.props.required}
             disabled={this.props.disabled} />
    );
  }
}

export default ISODurationInput;
