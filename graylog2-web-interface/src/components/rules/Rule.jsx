import PropTypes from 'prop-types';
import React from 'react';
import { LinkContainer } from 'react-router-bootstrap';

import { Row, Col, Button } from 'components/graylog';
import { PageHeader } from 'components/common';
import DocumentationLink from 'components/support/DocumentationLink';
import DocsHelper from 'util/DocsHelper';
import Routes from 'routing/Routes';

import RuleForm from './RuleForm';
import RuleHelper from './RuleHelper';

class Rule extends React.Component {
  static propTypes = {
    rule: PropTypes.object,
    usedInPipelines: PropTypes.array.isRequired,
    create: PropTypes.bool,
    onSave: PropTypes.func.isRequired,
    validateRule: PropTypes.func.isRequired,
  };

  static defaultProps = {
    rule: undefined,
    create: false,
  }

  render() {
    const { create, rule, usedInPipelines, onSave, validateRule } = this.props;
    let title;

    if (create) {
      title = 'Create pipeline rule';
    } else {
      title = <span>Pipeline rule <em>{rule.title}</em></span>;
    }

    return (
      <div>
        <PageHeader title={title}>
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
              <Button bsStyle="info" className="active">Manage rules</Button>
            </LinkContainer>
            &nbsp;
            <LinkContainer to={Routes.SYSTEM.PIPELINES.SIMULATOR}>
              <Button bsStyle="info">Simulator</Button>
            </LinkContainer>
          </span>
        </PageHeader>

        <Row className="content">
          <Col md={6}>
            <RuleForm rule={rule}
                      usedInPipelines={usedInPipelines}
                      create={create}
                      onSave={onSave}
                      validateRule={validateRule} />
          </Col>
          <Col md={6}>
            <RuleHelper />
          </Col>
        </Row>
      </div>
    );
  }
}

export default Rule;
