import PropTypes from 'prop-types';
import React from 'react';

const DocumentTitle = React.createClass({
  propTypes: {
    title: PropTypes.string.isRequired,
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
