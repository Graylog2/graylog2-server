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
import PropTypes from 'prop-types';
import React from 'react';

import Icon from './Icon';

/**
 * Adds an icon to an entity that was created by a content pack.
 */
class ContentPackMarker extends React.Component {
  static propTypes = {
    /** Content pack key of the entity's object. When set, the component will render the content pack marker. */
    contentPack: PropTypes.string,
    /** Margin-left the marker should use. */
    marginLeft: PropTypes.number,
    /** Margin-right the marker should use. */
    marginRight: PropTypes.number,
  };

  static defaultProps = { contentPack: undefined, marginLeft: 0, marginRight: 0 };

  render() {
    const style = { marginLeft: this.props.marginLeft, marginRight: this.props.marginRight };

    if (this.props.contentPack) {
      return <Icon name="cube" title="Created from content pack" style={style} />;
    }

    return null;
  }
}

export default ContentPackMarker;
