import PropTypes from 'prop-types';
import React from 'react';

import { Row, Col } from 'react-bootstrap';
import Timestamp from 'components/common/Timestamp';
import DateTime from 'logic/datetimes/DateTime';

import 'components/content-packs/ContentPackDetails.css';

const ContentPackInstallView = (props) => {
  const { comment } = props.install;
  const createdAt = props.install.created_at;
  const createdBy = props.install.created_by;
  return (<div>
    <Row>
      <Col smOffset={1}>
        <div>
          <dl className="deflist">
            <dt>Comment:</dt>
            <dd>{comment}</dd>
            <dt>Installed by:</dt>
            <dd>{createdBy}&nbsp;</dd>
            <dt>Installed at:</dt>
            <dd><Timestamp dateTime={createdAt} format={DateTime.Formats.COMPLETE} tz="browser" /></dd>
          </dl>
        </div>
      </Col>
    </Row>
  </div>);
};

ContentPackInstallView.propTypes = {
  install: PropTypes.object.isRequired,
};

export default ContentPackInstallView;
