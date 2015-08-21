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
    return (
      <div className="streamrule-connector-type-form">
        <OverlayTrigger
          placement="top"
          ref="savedTooltip"
          trigger="manual"
          defaultOverlayShown={false}
          overlay={<Tooltip>Saved!</Tooltip>}>
          <div>
            <Input type="radio" label="A message must mactch all of the following rules"
                   checked={this.props.stream.matching_type === 'AND'} onChange={this.handleTypeChangeToAnd.bind(this)}/>
            <Input type="radio" label="A message must match at least one of the following rules"
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
