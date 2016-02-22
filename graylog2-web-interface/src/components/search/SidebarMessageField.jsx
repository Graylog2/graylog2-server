import React, {PropTypes} from 'react';
import {Input, Button, ButtonGroup} from 'react-bootstrap';

const SidebarMessageField = React.createClass({
  propTypes: {
    field: PropTypes.object,
    fieldAnalyzers: React.PropTypes.array,
    onFieldAnalyzer: React.PropTypes.func,
    onToggled: PropTypes.func,
    selected: PropTypes.bool,
  },
  getInitialState() {
    return {
      showActions: false,
    };
  },
  _toggleShowActions() {
    this.setState({showActions: !this.state.showActions});
  },

  _fieldAnalyzerButtons() {
    return this.props.fieldAnalyzers.map((analyzer, idx) => {
      return (
        <Button key={'field-analyzer-button-' + idx} onClick={() => this.props.onFieldAnalyzer(analyzer.refId, this.props.field.name)}>
          {analyzer.displayName}
        </Button>
      );
    });
  },

  render() {
    let toggleClassName = 'fa fa-fw open-analyze-field ';
    toggleClassName += this.state.showActions ? 'open-analyze-field-active fa-caret-down' : 'fa-caret-right';

    return (
      <li>
        <div className="pull-left">
          <i className={toggleClassName}
             onClick={this._toggleShowActions}></i>
        </div>
        <div style={{marginLeft: 25}}>
          <Input type="checkbox"
                 label={this.props.field.name}
                 checked={this.props.selected}
                 onChange={() => this.props.onToggled(this.props.field.name)}/>

          {this.state.showActions &&
          <div className="analyze-field">
            <ButtonGroup bsSize="xsmall">
              {this._fieldAnalyzerButtons()}
            </ButtonGroup>
          </div>}
        </div>
      </li>
    );
  },
});

export default SidebarMessageField;
