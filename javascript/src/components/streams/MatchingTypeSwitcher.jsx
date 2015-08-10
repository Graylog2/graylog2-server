import React from 'react';
import { PropTypes, Component } from 'react';
import { Input } from 'react-bootstrap';
import StreamsStore from '../../stores/streams/StreamsStore';

class MatchingTypeSwitcher extends Component {
  static propTypes = {
    stream: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired,
  }
  render() {
    const valueLink = {
      value: this.props.stream.matching_type,
      requestChange: this.handleTypeChange,
      streamId: this.props.stream.id,
      onChange: this.props.onChange,
    };
    return (
      <div className="form-inline">
        A message needs to match{' '}
        <Input type="select" className="form-inline input-sm" valueLink={valueLink}>
          <option value="AND">all rules</option>
          <option value="OR">at least one rule</option>
        </Input>
        {' '}to be routed into this stream.{' '}
      </div>
    );
  }

  handleTypeChange(newValue) {
    StreamsStore.update(this.streamId, { 'matching_type': newValue }, this.onChange);
  }
}

export default MatchingTypeSwitcher;
