// @flow strict
import * as React from 'react';

import AuthenticationAction from 'actions/authentication/AuthenticationActions';
import {} from 'components/authentication/bindings'; // Bind all authentication plugins
import { Alert, Row, Col } from 'components/graylog';
import { DocumentTitle, PageHeader, Icon } from 'components/common';
import DocsHelper from 'util/DocsHelper';
import DocumentationLink from 'components/support/DocumentationLink';
import BackendsOverview from 'components/authentication/BackendsOverview';
import BackendOverviewLinks from 'components/authentication/BackendOverviewLinks';
import BackendActionLinks from 'components/authentication/BackendActionLinks';
import { useActiveBackend } from 'components/authentication/hooks';

const AuthenticationOverviewPage = () => {
  const { finishedLoading, activeBackend, backendsTotal } = useActiveBackend([AuthenticationAction.setActiveBackend]);

  return (
    <DocumentTitle title="All Authentication Services">
      <PageHeader title="All Authentication Services"
                  subactions={(
                    <BackendActionLinks activeBackend={activeBackend}
                                        finishedLoading={finishedLoading} />
                  )}>
        <span>Configure Graylog&apos;s authentication services of this Graylog cluster.</span>
        <span>Read more authentication in the <DocumentationLink page={DocsHelper.PAGES.USERS_ROLES}
                                                                 text="documentation" />.
        </span>
        <BackendOverviewLinks activeBackend={activeBackend}
                              finishedLoading={finishedLoading} />
      </PageHeader>
      {!!(backendsTotal && backendsTotal >= 1 && !activeBackend) && (
        <Row className="content">
          <Col xs={12}>
            <Alert bsStyle="warning">
              <Icon name="exclamation-circle" /> None of the configured authentication services is currently active.
            </Alert>
          </Col>
        </Row>
      )}
      <BackendsOverview />
    </DocumentTitle>
  );
};

export default AuthenticationOverviewPage;
