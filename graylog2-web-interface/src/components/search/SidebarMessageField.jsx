import React, {PropTypes} from 'react';

import {Input, DropdownButton, MenuItem} from 'react-bootstrap';
import naturalSort from 'javascript-natural-sort';

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
  _setShowActions(isOpen) {
    this.setState({showActions: isOpen});
  },

  _onFieldAnalyzer(refId, fieldName) {
    return () => {
      this.props.onFieldAnalyzer(refId, fieldName);
    };
  },

  _fieldAnalyzerMenuItems() {
    return this.props.fieldAnalyzers
      .sort((a, b) => naturalSort(a.displayName, b.displayName))
      .map((analyzer, idx) => {
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
                          onToggle={this._setShowActions}
                          title={<i className={toggleClassName} />}>
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
