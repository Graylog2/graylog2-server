import PropTypes from 'prop-types';
import React from 'react';

import { Row, Col } from 'react-bootstrap';

import 'components/content-packs/ContentPackDetails.css';

class ContentPackInstallView extends React.Component {
  static propTypes = {
    install: PropTypes.object.isRequired,
  };


  render() {
    const { comment, created_at, created_by, entities } = this.props.install;
    return (<div>
      <Row>
        <Col smOffset={1}>
          <div>
            <dl className="deflist">
              <dt>Comment:</dt> <dd>{comment}</dd>
              <dt>Installed by:</dt> <dd>{created_by}&nbsp;</dd>
              <dt>Installed at:</dt> <dd>{created_at}&nbsp;</dd>
            </dl>
          </div>
        </Col>
      </Row>
    </div>);
  }
}

export default ContentPackInstallView;
