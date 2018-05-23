import PropTypes from 'prop-types';
import React from 'react';

import { Row, Col, Button } from 'react-bootstrap';
import ContentPackDetails from 'components/content-packs/ContentPackDetails';
import ContentPackConstraints from 'components/content-packs/ContentPackConstraints';

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
    return (<a download={filename} href={href} ><Button id="download" bsStyle="info">Download</Button></a>);
  }

  render() {
    return (
      <div>
        <Row>
          <Col sm={6}>
            <ContentPackDetails contentPack={this.props.contentPack} verbose />
          </Col>
          <Col sm={6}>
            <ContentPackConstraints constraints={this.props.contentPack.requires} isFullFilled />
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
