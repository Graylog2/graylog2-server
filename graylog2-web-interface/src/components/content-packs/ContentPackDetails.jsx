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
    const markdownDescription = markdown.toHTML(this.props.contentPack.description || '');

    return (
      <div id="content-pack-details">
        <h2>Details</h2><small>Version: {this.props.contentPack.rev}</small>
        <div>
          <dl className="deflist">
            <dt>Name:</dt> <dd>{this.props.contentPack.name}&nbsp;</dd>
            <dt>Summary:</dt> <dd>{this.props.contentPack.summary}&nbsp;</dd>
            <dt>Vendor:</dt> <dd>{this.props.contentPack.vendor}&nbsp;</dd>
            <dt>URL:</dt> <dd><a href={this.props.contentPack.url}>{this.props.contentPack.url}</a>&nbsp;</dd>
            { this.props.contentPack.id && (<span><dt>ID:</dt> <dd><code>{this.props.contentPack.id}</code></dd></span>) }
            <dt>Parameters:</dt><dd>{this.props.contentPack.parameters.length}</dd>
          </dl>
        </div>
        <br />
        { this.props.contentPack.status && <ContentPackStatus states={this.props.contentPack.states} /> }
        <br />
        <br />
        { this.props.contentPack.constraints &&
        <div>
          <h3>Constrains</h3>
          <br />
          <ContentPackConstraints constraints={this.props.contentPack.constraints} />
          <br />
        </div>
        }
        <h3>Description</h3>
        <br />
        <div dangerouslySetInnerHTML={{ __html: markdownDescription }} />
      </div>
    );
  }
}

export default ContentPackDetails;
