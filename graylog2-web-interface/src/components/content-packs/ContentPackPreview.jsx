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
        <Button id="create" onClick={this.props.onSave}>Create</Button>
      </div>
    );
  }
}

export default ContentPackPreview;
