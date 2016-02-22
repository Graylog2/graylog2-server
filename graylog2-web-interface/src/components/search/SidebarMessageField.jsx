import React, {PropTypes} from 'react';
import {Input, Button, ButtonGroup, DropdownButton, MenuItem} from 'react-bootstrap';

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

  _onFieldAnalyzer(refId, fieldName) {
    return () => {
      this.props.onFieldAnalyzer(refId, fieldName);
      this._toggleShowActions();
    };
  },

  _fieldAnalyzerMenuItems() {
    return this.props.fieldAnalyzers.map((analyzer, idx) => {
      return (
        <MenuItem key={'field-analyzer-button-' + idx}
                  onClick={this._onFieldAnalyzer(analyzer.refId, this.props.field.name)}>
          {analyzer.displayName}
        </MenuItem>
      );
    });
  },

  render() {
    let toggleClassName = 'fa fa-fw open-analyze-field ';
    toggleClassName += this.state.showActions ? 'open-analyze-field-active fa-caret-down' : 'fa-caret-right';

    return (
      <li>
        <div className="pull-left">
          <DropdownButton bsStyle="link"
                          id={'field-analyzers-' + this.props.field.name}
                          title={<i className={toggleClassName}
                          onClick={this._toggleShowActions} />}>
            {this._fieldAnalyzerMenuItems()}
          </DropdownButton>
        </div>
        <div style={{marginLeft: 25}}>
          <Input type="checkbox"
                 label={this.props.field.name}
                 checked={this.props.selected}
                 onChange={() => this.props.onToggled(this.props.field.name)}/>
        </div>
      </li>
    );
  },
});

export default SidebarMessageField;
