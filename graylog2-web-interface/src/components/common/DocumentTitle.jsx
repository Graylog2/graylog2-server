import PropTypes from 'prop-types';
import React from 'react';

/**
 * React component that modifies the page `document.title` dynamically. When the component is unmounted, it
 * resets the title to the default (`Graylog`).
 */
const DocumentTitle = React.createClass({
  propTypes: {
    /** Title to prepend to the page `document.title`. */
    title: PropTypes.string.isRequired,
    /** Children to be rendered. */
    children: PropTypes.oneOfType([
      PropTypes.arrayOf(PropTypes.element),
      PropTypes.element,
    ]).isRequired,
  },

  componentDidMount() {
    document.title = `${document.title} - ${this.props.title}`;
  },

  componentWillUnmount() {
    document.title = this.defaultTitle;
  },

  defaultTitle: 'Graylog',
  render() {
    return this.props.children;
  },
});

export default DocumentTitle;
