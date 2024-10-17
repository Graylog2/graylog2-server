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
import React, { useEffect, useState } from 'react';

import ObjectUtils from 'util/ObjectUtils';
import connect from 'stores/connect';
import { PaginatedList, Spinner, SearchForm } from 'components/common';
import { Row, Col, Panel, Tabs, Tab } from 'components/bootstrap';
import DocumentationLink from 'components/support/DocumentationLink';
import withPaginationQueryParameter from 'components/common/withPaginationQueryParameter';
import DocsHelper from 'util/DocsHelper';
import { RulesActions, RulesStore } from 'stores/rules/RulesStore';

import { functionSignature } from './helpers';
import RuleHelperStyle from './RuleHelper.css';
import RuleHelperTable from './RulerHelperTable';

import type { BlockDict } from '../rule-builder/types';

const ruleTemplate = `rule "function howto"
when
  has_field("transaction_date")
then
  // the following date format assumes there's no time zone in the string
  let new_date = parse_date(to_string($message.transaction_date), "yyyy-MM-dd HH:mm:ss");
  set_field("transaction_year", new_date.year);
end`;

type Props = {
  functionDescriptors?: Array<BlockDict>
  paginationQueryParameter: any,
  hideExampleTab?: boolean
}

const RuleHelper = ({ functionDescriptors, paginationQueryParameter, hideExampleTab = false } : Props) => {
  const [expanded, setExpanded] = useState<{ [key: string]: boolean}>({});
  const [currentPage, setCurrentPage] = useState<number>(paginationQueryParameter.page);
  const [pageSize, setPageSize] = useState<number>(10);
  const [filteredDescriptors, setFilteredDescriptors] = useState<BlockDict[]|undefined>(undefined);

  useEffect(() => {
    RulesActions.loadFunctions();
  }, []);

  const toggleFunctionDetail = (functionName: string) => {
    const newState = ObjectUtils.clone(expanded);

    newState[functionName] = !newState[functionName];

    setExpanded(newState);
  };

  const onPageChange = (newPage: number, newPageSize: number) => {
    setCurrentPage(newPage);
    setPageSize(newPageSize);
  };

  const filterDescriptors = (filter: string) => {
    paginationQueryParameter.resetPage();

    if (!functionDescriptors) {
      return;
    }

    if (filter.length <= 0) {
      setFilteredDescriptors(functionDescriptors);
      setCurrentPage(1);

      return;
    }

    const filteredDescriptiors = functionDescriptors.filter((descriptor) => {
      const regexp = RegExp(filter);

      return regexp.test(functionSignature(descriptor)) || regexp.test(descriptor.description);
    });

    setFilteredDescriptors(filteredDescriptiors);
    setCurrentPage(1);
  };

  const onFilterReset = () => {
    paginationQueryParameter.resetPage();

    setFilteredDescriptors(functionDescriptors);
    setCurrentPage(1);
  };

  if (!functionDescriptors) {
    return <Spinner />;
  }

  const ruleDescriptors = filteredDescriptors || functionDescriptors;
  const pagedEntries = ruleDescriptors.slice((currentPage - 1) * pageSize, currentPage * pageSize);

  return (
    <Panel header="Rules quick reference">
      <Row className="row-sm rule-ref-descriptions">
        <Col md={12}>
          <p className={RuleHelperStyle.marginQuickReferenceText}>
            Read the <DocumentationLink page={DocsHelper.PAGES.PIPELINE_RULES}
                                        text="full documentation" />{' '}
            to gain a better understanding of how Graylog pipeline rules work.
          </p>
        </Col>
      </Row>
      <Row className="row-sm">
        {hideExampleTab ? (
          <Col sm={12}>
            <SearchForm onSearch={filterDescriptors}
                        topMargin={0}
                        onReset={onFilterReset} />
            <div className={`table-responsive ${RuleHelperStyle.marginTab} ref-rule`}>
              <PaginatedList totalItems={ruleDescriptors.length}
                             pageSize={pageSize}
                             onChange={onPageChange}
                             showPageSizeSelect={false}>
                <RuleHelperTable entries={pagedEntries}
                                 expanded={expanded}
                                 onFunctionClick={toggleFunctionDetail} />
              </PaginatedList>
            </div>
          </Col>
        ) : (
          <Col md={12}>
            <Tabs id="functionsHelper" defaultActiveKey={1} animation={false}>
              <Tab eventKey={1} title="Functions">
                <Row className="rule-ref-descriptions">
                  <Col sm={12}>
                    <p className={RuleHelperStyle.marginTab}>
                      This is a list of all available functions in pipeline rules. Click on a row to see more information
                      about the function parameters.
                    </p>
                  </Col>
                </Row>
                <Row>
                  <Col sm={12}>
                    <SearchForm onSearch={filterDescriptors}
                                label="Filter rules"
                                topMargin={0}
                                onReset={onFilterReset} />
                    <div className={`table-responsive ${RuleHelperStyle.marginTab} ref-rule`}>
                      <PaginatedList totalItems={ruleDescriptors.length}
                                     pageSize={pageSize}
                                     onChange={onPageChange}
                                     showPageSizeSelect={false}>
                        <RuleHelperTable entries={pagedEntries}
                                         expanded={expanded}
                                         onFunctionClick={toggleFunctionDetail} />
                      </PaginatedList>
                    </div>
                  </Col>
                </Row>
              </Tab>
              <Tab eventKey={2} title="Example">
                <p className={RuleHelperStyle.marginTab}>
                  Do you want to see how a pipeline rule looks like? Take a look at this example:
                </p>
                <pre className={`${RuleHelperStyle.marginTab} ${RuleHelperStyle.exampleFunction}`}>
                  {ruleTemplate}
                </pre>
              </Tab>
            </Tabs>
          </Col>
        )}
      </Row>
    </Panel>
  );
};

export default connect(withPaginationQueryParameter(RuleHelper),
  { ruleStore: RulesStore },
  ({ ruleStore }) => ({ ...ruleStore }));
