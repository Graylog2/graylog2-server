/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import PropTypes from 'prop-types';
import React from 'react';

import { PageHeader } from 'components/common';
import { Row, Col } from 'components/bootstrap';
import DocumentationLink from 'components/support/DocumentationLink';
import DocsHelper from 'util/DocsHelper';

import RuleForm from './RuleForm';
import RuleHelper from './RuleHelper';

import PipelinesSubareaNavigation from '../pipelines/PipelinesSubareaNavigation';

const Rule = ({ create, title }) => {
  let pageTitle;

  if (create) {
    pageTitle = 'Create pipeline rule';
  } else {
    pageTitle = <span>Pipeline rule <em>{title}</em></span>;
  }

  return (
    <div>
      <PipelinesSubareaNavigation />
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
