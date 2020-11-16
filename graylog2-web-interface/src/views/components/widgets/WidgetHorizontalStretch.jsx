/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
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
    const icon = stretched ? 'compress' : 'arrows-alt-h';
    const title = stretched ? 'Compress width' : 'Stretch width';

    return (
      <IconButton onClick={this._onClick} name={icon} title={title} />
    );
  }
}

export default WidgetHorizontalStretch;
