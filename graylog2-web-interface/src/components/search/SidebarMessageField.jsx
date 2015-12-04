import React, {PropTypes} from 'react';
import {Input, Button, ButtonGroup} from 'react-bootstrap';

const SidebarMessageField = React.createClass({
  propTypes: {
    field: PropTypes.object,
    onFieldSelectedForGraph: PropTypes.func.isRequired,
    onFieldSelectedForQuickValues: PropTypes.func.isRequired,
    onFieldSelectedForStats: PropTypes.func.isRequired,
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
              <Button onClick={() => this.props.onFieldSelectedForStats(this.props.field.name)}>
                Statistics
              </Button>
              <Button onClick={() => this.props.onFieldSelectedForQuickValues(this.props.field.name)}>
                Quick values
              </Button>
              <Button onClick={() => this.props.onFieldSelectedForGraph(this.props.field.name)}>
                Generate chart
              </Button>
            </ButtonGroup>
          </div>}
        </div>
      </li>
    );
  },
});

export default SidebarMessageField;
