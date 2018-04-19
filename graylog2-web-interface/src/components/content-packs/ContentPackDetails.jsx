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
    const contentPack = this.props.contentPack;

    return (
      <div id="content-pack-details">
        <h2>Details</h2><small>Version: {contentPack.rev}</small>
        <div>
          <dl className="deflist">
            <dt>Name:</dt> <dd>{contentPack.name}&nbsp;</dd>
            <dt>Summary:</dt> <dd>{contentPack.summary}&nbsp;</dd>
            <dt>Vendor:</dt> <dd>{contentPack.vendor}&nbsp;</dd>
            <dt>URL:</dt> <dd><a href={contentPack.url}>{contentPack.url}</a>&nbsp;</dd>
            { contentPack.id && (<span><dt>ID:</dt> <dd><code>{contentPack.id}</code></dd></span>) }
            { contentPack.parameters && (<span><dt>Parameters:</dt> <dd>{contentPack.parameters.length}</dd></span>) }
            { contentPack.entities && (<span><dt>Entities:</dt> <dd>{contentPack.entities.length}</dd></span>) }
          </dl>
        </div>
        <br />
        { contentPack.status && <ContentPackStatus states={contentPack.states} /> }
        <br />
        <br />
        { contentPack.constraints &&
        <div>
          <h3>Constrains</h3>
          <br />
          <ContentPackConstraints constraints={contentPack.constraints} />
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
