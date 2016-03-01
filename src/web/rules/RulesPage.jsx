import React from 'react';
import { Row, Col } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';

import PageHeader from 'components/common/PageHeader';
import DocumentationLink from 'components/support/DocumentationLink';

import DocsHelper from 'util/DocsHelper';

const RulesPage = React.createClass({

  render() {
    return (
      <span>
        <PageHeader title="Pipeline Rules">
          <span>
            Rules are a way of applying changes to messages in Graylog. A rule consists of a condition and a list of actions.
            Graylog evaluates the condition against a message and executes the actions if the condition is satisfied.
          </span>

          <span>
            Read more about Graylog pipeline rules in the <DocumentationLink page={"TODO"}
                                                                    text="documentation"/>.
          </span>
        </PageHeader>

        <Row className="content">
          <Col md={12}>
            <span>TODO</span>
          </Col>
        </Row>
      </span>
    );
  },

});

export default RulesPage;