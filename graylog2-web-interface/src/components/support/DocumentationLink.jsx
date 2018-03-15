import PropTypes from 'prop-types';
import React from 'react';
import DocsHelper from 'util/DocsHelper';

class DocumentationLink extends React.Component {
  static propTypes = {
    page: PropTypes.string.isRequired,
    text: PropTypes.node.isRequired,
    title: PropTypes.string,
  };

  render() {
    return (
      <a href={DocsHelper.toString(this.props.page)} title={this.props.title} target="_blank">
        {this.props.text}
      </a>
    );
  }
}

export default DocumentationLink;
