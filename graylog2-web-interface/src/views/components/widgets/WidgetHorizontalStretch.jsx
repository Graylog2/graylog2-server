import React from 'react';
import PropTypes from 'prop-types';

import { widgetDefinition } from 'views/logic/Widgets';
import { IconButton } from 'components/common';

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
    const stretched = width === Infinity;
    const icon = stretched ? 'compress' : 'arrows-h';
    const title = stretched ? 'Compress width' : 'Stretch width';
    return (
      <IconButton onClick={this._onClick} name={icon} title={title} />
    );
  }
}

export default WidgetHorizontalStretch;
