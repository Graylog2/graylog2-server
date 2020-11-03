// @flow strict
import * as React from 'react';

import { Alert, Col, Row } from 'components/graylog';
import User from 'logic/users/User';

type Props = {
  fullName: $PropertyType<User, 'fullName'>,
};

const ReadOnlyWarning = ({ fullName }: Props) => (
  <Row className="content">
    <Col xs={12}>
      <Alert bsStyle="danger">
        The selected user {fullName} can&apos;t be edited.
      </Alert>
    </Col>
  </Row>
);

export default ReadOnlyWarning;
