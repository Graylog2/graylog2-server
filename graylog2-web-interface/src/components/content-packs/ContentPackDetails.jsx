/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import PropTypes from 'prop-types';
import React from 'react';
import marked from 'marked';
import DOMPurify from 'dompurify';

import { Col, Row, Well } from 'components/graylog';
import ContentPackStatus from 'components/content-packs/ContentPackStatus';
import ContentPackConstraints from 'components/content-packs/ContentPackConstraints';
import ContentPackEntitiesList from 'components/content-packs/ContentPackEntitiesList';
import ContentPackParameterList from 'components/content-packs/ContentPackParameterList';
import 'components/content-packs/ContentPackDetails.css';
import { hasAcceptedProtocol } from 'util/URLUtils';

const ContentPackDetails = (props) => {
  const { contentPack, offset, verbose, constraints, showConstraints } = props;
  const markdownDescription = DOMPurify.sanitize(marked(contentPack.description || ''));
  let contentPackAnchor = contentPack.url;

  try {
    if (hasAcceptedProtocol(contentPack.url)) {
      contentPackAnchor = <a href={contentPack.url}>{contentPack.url}</a>;
    }
  } catch (e) {
    // Do nothing
  }

  return (
    <Row>
      <Col smOffset={offset} sm={9}>
        <div id="content-pack-details">
          <h2>Details</h2>
          <br />
          <div>
            <dl className="deflist">
              <dt>Version:</dt> <dd>{contentPack.rev}</dd>
              <dt>Name:</dt> <dd>{contentPack.name}&nbsp;</dd>
              <dt>Summary:</dt> <dd>{contentPack.summary}&nbsp;</dd>
              <dt>Vendor:</dt> <dd>{contentPack.vendor}&nbsp;</dd>
              <dt>URL:</dt> <dd>{contentPackAnchor}&nbsp;</dd>
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
          )}
          { contentPack.entities && contentPack.entities.length > 0 && verbose
          && <ContentPackEntitiesList contentPack={contentPack} readOnly />}
          { contentPack.parameters && contentPack.parameters.length > 0 && verbose
          && <ContentPackParameterList contentPack={contentPack} readOnly />}
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
