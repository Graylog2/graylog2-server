import PropTypes from 'prop-types';
import React from 'react';

import { markdown } from 'markdown';
import { Col } from 'react-bootstrap';

import ContentPackStatus from 'components/content-packs/ContentPackStatus';
import ContentPackConstraints from 'components/content-packs/ContentPackConstraints';
import ContentPackDetailsStyle from 'components/content-packs/ContentPackDetails.css';

class ContentPackDetails extends React.Component {
  static propTypes = {
    contentPack: PropTypes.object.isRequired,
  };

  render() {
    const markdownDescription = markdown.toHTML(this.props.contentPack.description);

    return (
      <div id="content-pack-details">
        <h2>Details</h2><small>Version: {this.props.contentPack.version}</small>
        <dl className="deflist">
          <dt>Vendor:</dt> <dd>{this.props.contentPack.vendor}</dd>
          <dt>URL:</dt> <dd><a href={this.props.contentPack.url}>{this.props.contentPack.url}</a></dd>
          <dt>ID:</dt> <dd><code>{this.props.contentPack.id}</code></dd>
        </dl>
        <ContentPackStatus states={this.props.contentPack.states} />
        <br />
        <br />
        <h2>Constrains</h2>
        <br />
        <ContentPackConstraints constraints={this.props.contentPack.constraints} />
        <br />
        <h2>Description</h2>
        <br />
        <div dangerouslySetInnerHTML={{ __html: markdownDescription }} />
      </div>
    );
  }
}

export default ContentPackDetails;
