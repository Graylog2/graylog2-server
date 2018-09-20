import PropTypes from 'prop-types';
import React from 'react';

import { Row, Col } from 'react-bootstrap';
import { DataTable, Timestamp } from 'components/common';
import DateTime from 'logic/datetimes/DateTime';

import 'components/content-packs/ContentPackDetails.css';

const ContentPackInstallView = (props) => {
  const rowFormatter = entity => (<tr><td>{entity.title}</td><td>{entity.type.name}</td></tr>);
  const { comment } = props.install;
  const createdAt = props.install.created_at;
  const createdBy = props.install.created_by;
  const headers = ['Title', 'Type'];
  return (<div>
    <Row>
      <Col smOffset={1} sm={10}>
        <h3>General information</h3>
        <dl className="deflist">
          <dt>Comment:</dt>
          <dd>{comment}</dd>
          <dt>Installed by:</dt>
          <dd>{createdBy}&nbsp;</dd>
          <dt>Installed at:</dt>
          <dd><Timestamp dateTime={createdAt} format={DateTime.Formats.COMPLETE} tz="browser" /></dd>
        </dl>
      </Col>
    </Row>
    <Row>
      <Col smOffset={1} sm={10}>
        <h3>Installed Entities</h3>
        <DataTable
          id="installed-entities"
          headers={headers}
          sortByKey="title"
          dataRowFormatter={rowFormatter}
          rows={props.install.entities}
          filterKeys={[]}
        />
      </Col>
    </Row>
  </div>);
};

ContentPackInstallView.propTypes = {
  install: PropTypes.object.isRequired,
};

export default ContentPackInstallView;
