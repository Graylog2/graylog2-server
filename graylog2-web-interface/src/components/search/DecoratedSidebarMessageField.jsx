import PropTypes from 'prop-types';
import React from 'react';
import { Input } from 'components/bootstrap';

import { DecoratedMessageFieldMarker } from 'components/search';

import DecoratorStyles from '!style!css!components/search/decoratorStyles.css';

class DecoratedSidebarMessageField extends React.Component {
  static propTypes = {
    field: PropTypes.object,
    onToggled: PropTypes.func,
    selected: PropTypes.bool,
  };

  render() {
    const label = (<span>
      {this.props.field.name}
    </span>);
    return (
      <li>
        <div className="pull-left" />
        <div className="field-selector">
          <Input id="decorator-field-checkbox"
                 type="checkbox"
                 label={label}
                 groupClassName={DecoratorStyles.decoratorFieldWrapper}
                 checked={this.props.selected}
                 onChange={() => this.props.onToggled(this.props.field.name)} />
          <DecoratedMessageFieldMarker className={DecoratorStyles.decoratorMarkerSidebar} />
        </div>
      </li>
    );
  }
}

export default DecoratedSidebarMessageField;
