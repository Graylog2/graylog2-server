import React from 'react';

const DocumentTitle = React.createClass({
  propTypes: {
    title: React.PropTypes.string.isRequired,
    children: React.PropTypes.oneOfType([
      React.PropTypes.arrayOf(React.PropTypes.element),
      React.PropTypes.element,
    ]).isRequired,
  },

  componentDidMount() {
    document.title = `USAM - ${this.props.title}`;
  },

  componentWillUnmount() {
    document.title = this.defaultTitle;
  },

  defaultTitle: '',
  render() {
    return this.props.children;
  },
});

export default DocumentTitle;
