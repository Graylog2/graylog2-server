import React from 'react';
import PropTypes from 'prop-types';

import { widgetDefinition } from 'views/logic/Widgets';
import style from './WidgetHorizontalStretch.css';

class WidgetHorizontalStretch extends React.Component {
  static propTypes = {
    widgetId: PropTypes.string.isRequired,
    widgetType: PropTypes.string.isRequired,
    position: PropTypes.object.isRequired,
    onStretch: PropTypes.func.isRequired,
  };

  _onClick = () => {
    const { onStretch, position, widgetId, widgetType } = this.props;
    const { col, row, height, width } = position;
    const { defaultWidth } = widgetDefinition(widgetType);
    onStretch({
      id: widgetId, col: col, row: row, height: height, width: width === Infinity ? defaultWidth : Infinity,
    });
  };

  render() {
    const { position } = this.props;
    const { width } = position;
    const icon = width === Infinity ? 'compress' : 'arrows-h';
    return (
      <span>
        {/* eslint-disable-next-line jsx-a11y/click-events-have-key-events */}
        <i role="link" tabIndex={0} onClick={this._onClick} className={`fa fa-${icon} ${style.button}`} />
      </span>
    );
  }
}

export default WidgetHorizontalStretch;
