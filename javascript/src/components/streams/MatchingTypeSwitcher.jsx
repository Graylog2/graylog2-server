import React from 'react';
import { PropTypes, Component } from 'react';
import { Input, OverlayTrigger, Tooltip } from 'react-bootstrap';
import StreamsStore from '../../stores/streams/StreamsStore';

class MatchingTypeSwitcher extends Component {
  static propTypes = {
    stream: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired,
  }
  render() {
    const valueLink = {
      value: this.props.stream.matching_type,
      requestChange: this.handleTypeChange.bind(this),
    };
    return (
      <div className="form-inline">
        A message needs to match{' '}
        <OverlayTrigger
          placement="top"
          ref="savedTooltip"
          trigger="manual"
          defaultOverlayShown={false}
          overlay={<Tooltip>Saved!</Tooltip>}>
          <Input type="select" className="form-inline input-sm" valueLink={valueLink}>
            <option value="AND">all rules</option>
            <option value="OR">at least one rule</option>
          </Input>
        </OverlayTrigger>
        {' '}to be routed into this stream.{' '}
      </div>
    );
  }

  handleTypeChange(newValue) {
    StreamsStore.update(this.props.stream.id, { 'matching_type': newValue }, () => {
      this.props.onChange();
      this.refs.savedTooltip.show();
      window.setTimeout(() => this.refs.savedTooltip.hide(), 1000);
    });
  }
}

export default MatchingTypeSwitcher;
