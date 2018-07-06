import React from 'react';
import PropTypes from 'prop-types';

import { widgetDefinition } from 'enterprise/logic/Widget';
import style from './WidgetHorizontalStretch.css';

class WidgetHorizontalStretch extends React.Component {
  static propTypes = {
    widgetId: PropTypes.string.isRequired,
    widgetType: PropTypes.string.isRequired,
    position: PropTypes.object.isRequired,
    onStretch: PropTypes.func.isRequired,
  };

  _onClick = () => {
    const { col, row, height, width } = this.props.position;
    const defaultWidth = widgetDefinition(this.props.widgetType).defaultWidth;
    this.props.onStretch({
      id: this.props.widgetId, col: col, row: row, height: height, width: width === Infinity ? defaultWidth : Infinity,
    });
  };

  render() {
    const { width } = this.props.position;
    const icon = width === Infinity ? 'compress' : 'arrows-h';
    return (
      <span>
        <i role="link" tabIndex={0} onClick={this._onClick} className={`fa fa-${icon} ${style.button}`} />
      </span>
    );
  }
}

export default WidgetHorizontalStretch;
