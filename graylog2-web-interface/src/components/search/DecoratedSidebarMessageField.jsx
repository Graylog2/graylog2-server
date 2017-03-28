import React from 'react';
import { Input } from 'components/bootstrap';

import { DecoratedMessageFieldMarker } from 'components/search';

import DecoratorStyles from '!style!css!components/search/decoratorStyles.css';

const DecoratedSidebarMessageField = React.createClass({
  propTypes: {
    field: React.PropTypes.object,
    onToggled: React.PropTypes.func,
    selected: React.PropTypes.bool,
  },
  render() {
    const label = (<span>
      {this.props.field.name}
    </span>);
    return (
      <li>
        <div className="pull-left" />
        <div className="field-selector">
          <Input type="checkbox"
                 label={label}
                 groupClassName={DecoratorStyles.decoratorFieldWrapper}
                 checked={this.props.selected}
                 onChange={() => this.props.onToggled(this.props.field.name)} />
          <DecoratedMessageFieldMarker className={DecoratorStyles.decoratorMarkerSidebar} />
        </div>
      </li>
    );
  },
});

export default DecoratedSidebarMessageField;
