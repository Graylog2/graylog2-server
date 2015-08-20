import React from 'react';
import { PropTypes, Component } from 'react';
import { Input, OverlayTrigger, Tooltip } from 'react-bootstrap';
import StreamsStore from '../../stores/streams/StreamsStore';

class MatchingTypeSwitcher extends Component {
  static propTypes = {
    stream: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired,
  };
  render() {
    const valueLink = {
      value: this.props.stream.matching_type,
      requestChange: this.handleTypeChange.bind(this),
    };
    return (
      <div>
        A message needs to match{' '}
        <OverlayTrigger
          placement="top"
          ref="savedTooltip"
          trigger="manual"
          defaultOverlayShown={false}
          overlay={<Tooltip>Saved!</Tooltip>}>
          <div>
            <Input type="radio" label="Matching all of the following rules"
                   checked={this.props.stream.matching_type === 'AND'} onChange={this.handleTypeChangeToAnd.bind(this)}/>
            <Input type="radio" label="Matching at least one of the following rules"
                   checked={this.props.stream.matching_type === 'OR'} onChange={this.handleTypeChangeToOr.bind(this)}/>
          </div>
        </OverlayTrigger>
      </div>
    );
  }

  handleTypeChangeToAnd() {
    this.handleTypeChange("AND");
  }

  handleTypeChangeToOr() {
    this.handleTypeChange("OR");
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
