// @flow strict
import React from 'react';
import PropTypes from 'prop-types';
import { withRouter } from 'react-router';

import { FetchError } from 'logic/rest/FetchProvider';

import { DocumentTitle, PageHeader, Icon } from 'components/common';
import { Alert, Row, Col } from 'components/graylog';
import ClipboardButton from 'components/common/ClipboardButton';
import AppContentGrid from 'components/layout/AppContentGrid';

type Props = {
  error: FetchError,
  location: {
    pathname: string
  }
}

const UnauthorizedErrorPage = ({ error, location: { pathname } }: Props) => {
  const errorMessage = error?.message || JSON.stringify(error);
  const pageDetails = `The error occurred while trying to access ${pathname}`;
  return (
    <DocumentTitle title="Missing Permissions">
      <AppContentGrid>
        <Row className="content">
          <Col md={12}>
            <PageHeader title="Missing Permissions" subpage>
              <span>You do not have the required permissions to view this ressource. Please contact your administrator and provide the error details.</span>
            </PageHeader>
            <Alert bsStyle="danger" role="alert">
              <p>
                <b><Icon name="info-circle" /> The permissions check for the following request failed</b>
              </p>
              <br />
              <pre>
                <div className="pull-right">
                  <ClipboardButton title={<Icon name="copy" fixedWidth />}
                                   bsSize="sm"
                                   text={`${errorMessage}\n${pageDetails}`}
                                   buttonTitle="Copy error details to clipboard" />
                </div>
                <p>{errorMessage}</p>
                <p>{pageDetails}</p>
              </pre>
            </Alert>
          </Col>
        </Row>
      </AppContentGrid>
    </DocumentTitle>
  );
};

UnauthorizedErrorPage.propTypes = {
  error: PropTypes.shape({
    message: PropTypes.string.isRequired,
  }).isRequired,
  location: PropTypes.shape({
    pathname: PropTypes.string,
  }).isRequired,
};

export default withRouter(UnauthorizedErrorPage);
