import PropTypes from 'prop-types';
import React from 'react';

import { markdown } from 'markdown';
import { Row, Col } from 'react-bootstrap';

import ContentPackStatus from 'components/content-packs/ContentPackStatus';
import ContentPackConstraints from 'components/content-packs/ContentPackConstraints';
import ContentPackEntitiesList from 'components/content-packs/ContentPackEntitiesList';
import ContentPackParameterList from 'components/content-packs/ContentPackParameterList';
import 'components/content-packs/ContentPackDetails.css';

class ContentPackDetails extends React.Component {
  static propTypes = {
    contentPack: PropTypes.object.isRequired,
    verbose: PropTypes.bool,
    offset: PropTypes.number,
    showConstraints: PropTypes.bool,
  };

  static defaultProps = {
    offset: 1,
    verbose: false,
    showConstraints: false,
  };

  render() {
    const markdownDescription = markdown.toHTML(this.props.contentPack.description || '');
    const contentPack = this.props.contentPack;

    return (
      <Row>
        <Col smOffset={this.props.offset} sm={9}>
          <div id="content-pack-details">
            <h2>Details</h2>
            <br />
            <div>
              <dl className="deflist">
                <dt>Version:</dt> <dd>{contentPack.rev}</dd>
                <dt>Name:</dt> <dd>{contentPack.name}&nbsp;</dd>
                <dt>Summary:</dt> <dd>{contentPack.summary}&nbsp;</dd>
                <dt>Vendor:</dt> <dd>{contentPack.vendor}&nbsp;</dd>
                <dt>URL:</dt> <dd><a href={contentPack.url}>{contentPack.url}</a>&nbsp;</dd>
                { contentPack.id && (<span><dt>ID:</dt> <dd><code>{contentPack.id}</code></dd></span>) }
                { contentPack.parameters && !this.props.verbose && (<span><dt>Parameters:</dt> <dd>{contentPack.parameters.length}</dd></span>) }
                { contentPack.entities && !this.props.verbose && (<span><dt>Entities:</dt> <dd>{contentPack.entities.length}</dd></span>) }
              </dl>
            </div>
            <br />
            { contentPack.status && <ContentPackStatus states={contentPack.states} /> }
            <br />
            <br />
            { contentPack.requires && this.props.showConstraints &&
            <div>
              <h3>Constrains</h3>
              <br />
              <ContentPackConstraints constraints={contentPack.requires} />
              <br />
            </div>
            }
            { contentPack.entities && this.props.verbose &&
              <ContentPackEntitiesList contentPack={this.props.contentPack} readOnly />
            }
            { contentPack.parameters && this.props.verbose &&
              <ContentPackParameterList contentPack={this.props.contentPack} readOnly />
            }
            <h3>Description</h3>
            <br />
            <div dangerouslySetInnerHTML={{ __html: markdownDescription }} />
          </div>
        </Col>
      </Row>
    );
  }
}

export default ContentPackDetails;
