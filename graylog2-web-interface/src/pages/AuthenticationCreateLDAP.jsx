// @flow strict
import * as React from 'react';

import { Row, Col } from 'components/graylog';
import DocsHelper from 'util/DocsHelper';
import Wizard from 'components/common/Wizard';
import PageHeader from 'components/common/PageHeader';
import DocumentationLink from 'components/support/DocumentationLink';

const AuthenticationCreateLDAP = () => {
  return (
    <>
      <PageHeader title="Create LDAP Authentication Provider">
        <span>Configure Graylog&apos;s authentication providers of this Graylog cluster.</span>
        <span>
          Read more authentication in the <DocumentationLink page={DocsHelper.PAGES.USERS_ROLES}
                                                             text="documentation" />.
        </span>
      </PageHeader>

      <Row className="content">
        <Col col={12}>
          <Wizard horizontal
                  justified
                  hidePreviousNextButtons />
        </Col>
      </Row>
    </>
  );
};

export default AuthenticationCreateLDAP;
