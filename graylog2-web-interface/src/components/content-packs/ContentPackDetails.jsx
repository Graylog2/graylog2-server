import PropTypes from 'prop-types';
import React from 'react';

import { markdown } from 'markdown';
import { Row, Col, Well } from 'react-bootstrap';

import ContentPackStatus from 'components/content-packs/ContentPackStatus';
import ContentPackConstraints from 'components/content-packs/ContentPackConstraints';
import ContentPackEntitiesList from 'components/content-packs/ContentPackEntitiesList';
import ContentPackParameterList from 'components/content-packs/ContentPackParameterList';
import 'components/content-packs/ContentPackDetails.css';

const ContentPackDetails = (props) => {
  const { contentPack, constraints, showConstraints, offset, verbose } = props;
  const markdownDescription = markdown.toHTML(contentPack.description || '');

  return (
    <Row>
      <Col smOffset={offset} sm={9}>
        <div id="content-pack-details">
          <h2>Details</h2>
          <br />
          <div>
            <h3>{contentPack.name}</h3>
            <dl className="deflist">
              <dt>Version:</dt> <dd>{contentPack.rev}</dd>
              <dt>Name:</dt> <dd>{contentPack.name}&nbsp;</dd>
              <dt>Summary:</dt> <dd>{contentPack.summary}&nbsp;</dd>
              <dt>Vendor:</dt> <dd>{contentPack.vendor}&nbsp;</dd>
              <dt>URL:</dt> <dd>{contentPack.url}&nbsp;</dd>
              { contentPack.id && (<span><dt>ID:</dt> <dd><code>{contentPack.id}</code></dd></span>) }
              { contentPack.parameters && !verbose && (<span><dt>Parameters:</dt> <dd>{contentPack.parameters.length}</dd></span>) }
              { contentPack.entities && !verbose && (<span><dt>Entities:</dt> <dd>{contentPack.entities.length}</dd></span>) }
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
          { contentPack.constraints && showConstraints
          && (
          <div>
            <ContentPackConstraints constraints={constraints} />
            <br />
          </div>
          )
          }
          { contentPack.entities && contentPack.entities.length > 0 && verbose
          && <ContentPackEntitiesList contentPack={contentPack} readOnly />
          }
          { contentPack.parameters && contentPack.parameters.length > 0 && verbose
          && <ContentPackParameterList contentPack={contentPack} readOnly />
          }
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
