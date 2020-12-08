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

/**
 * React component that modifies the page `document.title` dynamically. When the component is unmounted, it
 * resets the title to the default (`Graylog`).
 *
 * Example:
 *
 * ```js
 * <DocumentTitle title="This site is great">
 *   {contents}
 * </DocumentTitle>
 * ```
 */
class DocumentTitle extends React.Component {
  static propTypes = {
    /** Title to prepend to the page `document.title`. */
    title: PropTypes.string.isRequired,
    /** Children to be rendered. */
    children: PropTypes.oneOfType([
      PropTypes.arrayOf(PropTypes.element),
      PropTypes.element,
    ]).isRequired,
  };

  componentDidMount() {
    document.title = `${document.title} - ${this.props.title}`;
  }

  componentWillUnmount() {
    document.title = this.defaultTitle;
  }

  defaultTitle = 'Graylog';

  render() {
    return this.props.children;
  }
}

export default DocumentTitle;
