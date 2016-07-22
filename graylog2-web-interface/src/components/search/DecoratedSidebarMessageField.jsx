import React from 'react';
import { Input } from 'react-bootstrap';

const DecoratedSidebarMessageField = React.createClass({
  propTypes: {
    field: React.PropTypes.object,
    onToggled: React.PropTypes.func,
    selected: React.PropTypes.bool,
  },
  render() {
    const label = (<span>
      {this.props.field.name}
      {' '}
      <i className="fa fa-pencil"
         title="This field was added to the search result by a decorator and is not stored in any index. Therefore you cannot analyze it." />
    </span>);
    return (
      <li>
        <div className="pull-left">
        </div>
        <div className="field-selector">
          <Input type="checkbox"
                 label={label}
                 checked={this.props.selected}
                 onChange={() => this.props.onToggled(this.props.field.name)}/>
        </div>
      </li>
    );
  },
});

export default DecoratedSidebarMessageField;
