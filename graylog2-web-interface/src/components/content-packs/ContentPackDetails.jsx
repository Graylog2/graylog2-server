import PropTypes from 'prop-types';
import React from 'react';

import { markdown } from 'markdown';
import { Row, Col, Well } from 'components/graylog';

import ContentPackStatus from 'components/content-packs/ContentPackStatus';
import ContentPackConstraints from 'components/content-packs/ContentPackConstraints';
import ContentPackEntitiesList from 'components/content-packs/ContentPackEntitiesList';
import ContentPackParameterList from 'components/content-packs/ContentPackParameterList';
import 'components/content-packs/ContentPackDetails.css';

const ContentPackDetails = (props) => {
  const markdownDescription = markdown.toHTML(props.contentPack.description || '');
  const { contentPack } = props;
  const { constraints } = props;

  return (
    <Row>
      <Col smOffset={props.offset} sm={9}>
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
              { contentPack.parameters && !props.verbose && (<span><dt>Parameters:</dt> <dd>{contentPack.parameters.length}</dd></span>) }
              { contentPack.entities && !props.verbose && (<span><dt>Entities:</dt> <dd>{contentPack.entities.length}</dd></span>) }
            </dl>
          </div>
          { contentPack.description
          && (
          <div>
            <h2>Description</h2>
            <br />
            <Well>
              {/* eslint-disable-next-line react/no-danger */}
              <div dangerouslySetInnerHTML={{ __html: markdownDescription }} />
            </Well>
          </div>
          ) }
          <br />
          { contentPack.status && <ContentPackStatus states={contentPack.states} /> }
          <br />
          <br />
          { contentPack.constraints && props.showConstraints
          && (
          <div>
            <ContentPackConstraints constraints={constraints} />
            <br />
          </div>
          )}
          { contentPack.entities && contentPack.entities.length > 0 && props.verbose
          && <ContentPackEntitiesList contentPack={props.contentPack} readOnly />}
          { contentPack.parameters && contentPack.parameters.length > 0 && props.verbose
          && <ContentPackParameterList contentPack={props.contentPack} readOnly />}
        </div>
      </Col>
    </Row>
  );
};

ContentPackDetails.propTypes = {
  contentPack: PropTypes.object.isRequired,
  constraints: PropTypes.arrayOf(PropTypes.object),
  verbose: PropTypes.bool,
  offset: PropTypes.number,
  showConstraints: PropTypes.bool,
};

ContentPackDetails.defaultProps = {
  offset: 1,
  verbose: false,
  showConstraints: false,
  constraints: [],
};

export default ContentPackDetails;
