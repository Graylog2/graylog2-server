import PropTypes from 'prop-types';
import React from 'react';

import { LinkContainer } from 'components/graylog/router';
import { Row, Col, Button } from 'components/graylog';
import { PageHeader } from 'components/common';
import DocumentationLink from 'components/support/DocumentationLink';
import DocsHelper from 'util/DocsHelper';
import Routes from 'routing/Routes';

import RuleForm from './RuleForm';
import RuleHelper from './RuleHelper';

const Rule = ({ create, title }) => {
  let pageTitle;

  if (create) {
    pageTitle = 'Create pipeline rule';
  } else {
    pageTitle = <span>Pipeline rule <em>{title}</em></span>;
  }

  return (
    <div>
      <PageHeader title={pageTitle}>
        <span>
          Rules are a way of applying changes to messages in Graylog. A rule consists of a condition and a list{' '}
          of actions.{' '}
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
            <Button bsStyle="info">Manage rules</Button>
          </LinkContainer>
            &nbsp;
          <LinkContainer to={Routes.SYSTEM.PIPELINES.SIMULATOR}>
            <Button bsStyle="info">Simulator</Button>
          </LinkContainer>
        </span>
      </PageHeader>

      <Row className="content">
        <Col md={6}>
          <RuleForm create={create} />
        </Col>
        <Col md={6}>
          <RuleHelper />
        </Col>
      </Row>
    </div>
  );
};

Rule.propTypes = {
  title: PropTypes.string,
  create: PropTypes.bool,
};

Rule.defaultProps = {
  title: '',
  create: false,
};

export default Rule;
