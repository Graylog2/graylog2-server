import React from 'react';
import createReactClass from 'create-react-class';
import { LinkContainer } from 'react-router-bootstrap';
import Reflux from 'reflux';

import { Row, Col, Button } from 'components/graylog';
import { DocumentTitle, PageHeader } from 'components/common';
import DocumentationLink from 'components/support/DocumentationLink';
import DocsHelper from 'util/DocsHelper';
import RulesComponent from 'components/rules/RulesComponent';
import Routes from 'routing/Routes';
import CombinedProvider from 'injection/CombinedProvider';

const { RulesStore, RulesActions } = CombinedProvider.get('Rules');


const RulesPage = createReactClass({
  displayName: 'RulesPage',

  mixins: [
    Reflux.connect(RulesStore),
  ],

  componentDidMount() {
    RulesActions.list();
  },

  render() {
    return (
      <DocumentTitle title="Pipeline rules">
        <span>
          <PageHeader title="Pipeline Rules">
            <span>
              Rules are a way of applying changes to messages in Graylog. A rule consists of a condition and a list of actions.
              Graylog evaluates the condition against a message and executes the actions if the condition is satisfied.
            </span>

            <span>
              Read more about Graylog pipeline rules in the <DocumentationLink page={DocsHelper.PAGES.PIPELINE_RULES}
                                                                               text="documentation" />.
            </span>

            <span>
              <LinkContainer to={Routes.SYSTEM.PIPELINES.OVERVIEW}>
                <Button bsStyle="info">Manage pipelines</Button>
              </LinkContainer>
              &nbsp;
              <LinkContainer to={Routes.SYSTEM.PIPELINES.RULES}>
                <Button bsStyle="info" className="active">Manage rules</Button>
              </LinkContainer>
              &nbsp;
              <LinkContainer to={Routes.SYSTEM.PIPELINES.SIMULATOR}>
                <Button bsStyle="info">Simulator</Button>
              </LinkContainer>
            </span>
          </PageHeader>

          <Row className="content">
            <Col md={12}>
              <RulesComponent rules={this.state.rules} />
            </Col>
          </Row>
        </span>
      </DocumentTitle>
    );
  },
});

export default RulesPage;
