import React from 'react';

import { Timestamp } from 'components/common';
import { Row, Col } from 'components/bootstrap';

import 'components/content-packs/ContentPackDetails.css';
import ContentPackInstallEntityList from './ContentPackInstallEntityList';

type ContentPackInstallViewProps = {
  install: any;
};

const ContentPackInstallView = (props: ContentPackInstallViewProps) => {
  const { comment } = props.install;
  const createdAt = props.install.created_at;
  const createdBy = props.install.created_by;

  return (
    <div>
      <Row>
        <Col smOffset={1} sm={10}>
          <h3>General information</h3>
          <dl className="deflist">
            <dt>Comment:</dt>
            <dd>{comment}</dd>
            <dt>Installed by:</dt>
            <dd>{createdBy}&nbsp;</dd>
            <dt>Installed at:</dt>
            <dd><Timestamp dateTime={createdAt} /></dd>
          </dl>
        </Col>
      </Row>
      <Row>
        <Col smOffset={1} sm={10}>
          <ContentPackInstallEntityList entities={props.install.entities} />
        </Col>
      </Row>
    </div>
  );
};

export default ContentPackInstallView;
