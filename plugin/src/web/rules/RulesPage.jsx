import React from 'react';
import { Row, Col, Button } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';

import Reflux from 'reflux';

import PageHeader from 'components/common/PageHeader';
import DocumentationLink from 'components/support/DocumentationLink';

import DocsHelper from 'util/DocsHelper';

import RulesComponent from './RulesComponent';
import RulesStore from './RulesStore';
import RulesActions from './RulesActions';

import Routes from 'routing/Routes';

const RulesPage = React.createClass({
  mixins: [
    Reflux.connect(RulesStore),
  ],
  componentDidMount() {
    RulesActions.list();
  },

  render() {
    return (
      <span>
        <PageHeader title="Pipeline Rules" experimental>
          <span>
            Rules are a way of applying changes to messages in Graylog. A rule consists of a condition and a list of actions.
            Graylog evaluates the condition against a message and executes the actions if the condition is satisfied.
          </span>

          <span>
            Read more about Graylog pipeline rules in the <DocumentationLink page={DocsHelper.PAGES.PIPELINE_RULES}
                                                                    text="documentation" />.
          </span>

          <span>
            <LinkContainer to={Routes.pluginRoute('SYSTEM_PIPELINES')}>
              <Button bsStyle="info">Manage pipelines</Button>
            </LinkContainer>
          </span>
        </PageHeader>

        <Row className="content">
          <Col md={12}>
            <RulesComponent rules={this.state.rules}/>
          </Col>
        </Row>
      </span>
    );
  },
});

export default RulesPage;