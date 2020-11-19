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

import { Row, Col, Button } from 'components/graylog';
import ContentPackDetails from 'components/content-packs/ContentPackDetails';
import ContentPackConstraints from 'components/content-packs/ContentPackConstraints';

import ContentPackEntitiesList from './ContentPackEntitiesList';
import ContentPackParameterList from './ContentPackParameterList';

class ContentPackPreview extends React.Component {
  static propTypes = {
    contentPack: PropTypes.object.isRequired,
    onSave: PropTypes.func,
  };

  static defaultProps = {
    onSave: () => {},
  };

  _renderDownload() {
    const contentPackEncoded = encodeURIComponent(JSON.stringify(this.props.contentPack, null, 2));
    const href = `data:text/plain;charset=utf-8,${contentPackEncoded}`;
    const filename = `content-pack-${this.props.contentPack.id}-${this.props.contentPack.rev}.json`;

    return (
      <a download={filename} href={href}>
        <Button id="download" bsStyle="info" onClick={this.props.onSave}>
          Create and Download
        </Button>
      </a>
    );
  }

  render() {
    return (
      <div>
        <Row>
          <Col sm={6}>
            <ContentPackDetails contentPack={this.props.contentPack} />
          </Col>
          <Col sm={6}>
            <ContentPackConstraints constraints={this.props.contentPack.constraints} isFulfilled />
            <ContentPackEntitiesList contentPack={this.props.contentPack} readOnly />
            <ContentPackParameterList contentPack={this.props.contentPack} readOnly />
          </Col>
        </Row>
        <Row>
          <Col sm={6}>
            <Button id="create" bsStyle="primary" onClick={this.props.onSave}>Create</Button>&nbsp;
            {this._renderDownload()}
          </Col>
        </Row>
      </div>
    );
  }
}

export default ContentPackPreview;
