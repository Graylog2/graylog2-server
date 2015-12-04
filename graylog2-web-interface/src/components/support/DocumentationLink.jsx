import React from 'react';
import DocsHelper from 'util/DocsHelper';

const DocumentationLink = React.createClass({
  propTypes: {
    page: React.PropTypes.string.isRequired,
    text: React.PropTypes.node.isRequired,
    title: React.PropTypes.string,
  },
  render() {
    return (
      <a href={DocsHelper.toString(this.props.page)} title={this.props.title} target="_blank">
        {this.props.text}
      </a>
    );
  },
});

export default DocumentationLink;
