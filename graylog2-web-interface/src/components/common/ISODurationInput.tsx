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
  getValue(): string;
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
             ref={(isoDuration: DurationInput) => { this.isoDuration = isoDuration; }}
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
