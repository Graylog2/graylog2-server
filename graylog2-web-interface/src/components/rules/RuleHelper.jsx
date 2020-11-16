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
import React from 'react';
import PropTypes from 'prop-types';

import ObjectUtils from 'util/ObjectUtils';
import connect from 'stores/connect';
import { Row, Col, Panel, Table, Tabs, Tab } from 'components/graylog';
import { Icon, PaginatedList, Spinner, SearchForm } from 'components/common';
import DocumentationLink from 'components/support/DocumentationLink';
import DocsHelper from 'util/DocsHelper';
import CombinedProvider from 'injection/CombinedProvider';

import RuleHelperStyle from './RuleHelper.css';

const { RulesStore, RulesActions } = CombinedProvider.get('Rules');

const ruleTemplate = `rule "function howto"
when
  has_field("transaction_date")
then
  // the following date format assumes there's no time zone in the string
  let new_date = parse_date(to_string($message.transaction_date), "yyyy-MM-dd HH:mm:ss");
  set_field("transaction_year", new_date.year);
end`;

class RuleHelper extends React.Component {
  constructor(props) {
    super(props);

    this.state = {
      expanded: {},
      currentPage: 1,
      pageSize: 10,
      filteredDescriptors: undefined,
      pageBeforeFilter: undefined,
    };
  }

  componentDidMount() {
    RulesActions.loadFunctions();
  }

  _niceType = (typeName) => {
    return typeName.replace(/^.*\.(.*?)$/, '$1');
  };

  _toggleFunctionDetail = (functionName) => {
    const { expanded } = this.state;
    const newState = ObjectUtils.clone(expanded);

    newState[functionName] = !newState[functionName];

    this.setState({ expanded: newState });
  }

  _functionSignature = (descriptor) => {
    const args = descriptor.params.map((p) => (p.optional ? `[${p.name}]` : p.name));

    return `${descriptor.name}(${args.join(', ')}) : ${this._niceType(descriptor.return_type)}`;
  }

  _parameters = (descriptor) => {
    return descriptor.params.map((p) => {
      return (
        <tr key={p.name}>
          <td className={RuleHelperStyle.adjustedTableCellWidth}>{p.name}</td>
          <td className={RuleHelperStyle.adjustedTableCellWidth}>{this._niceType(p.type)}</td>
          <td className={`${RuleHelperStyle.adjustedTableCellWidth} text-centered`}>{p.optional ? null : <Icon name="check" />}</td>
          <td>{p.description}</td>
        </tr>
      );
    });
  }

  _renderFunctions = (descriptors) => {
    const { expanded } = this.state;

    if (!descriptors) {
      return [];
    }

    return descriptors.map((d) => {
      let details = null;

      if (expanded[d.name]) {
        details = (
          <tr>
            <td colSpan="2">
              <Table condensed striped hover>
                <thead>
                  <tr>
                    <th>Parameter</th>
                    <th>Type</th>
                    <th>Required</th>
                    <th>Description</th>
                  </tr>
                </thead>
                <tbody>
                  {this._parameters(d)}
                </tbody>
              </Table>
            </td>
          </tr>
        );
      }

      return (
        <tbody key={d.name}>
          <tr onClick={() => this._toggleFunctionDetail(d.name)} className={RuleHelperStyle.clickableRow}>
            <td className={RuleHelperStyle.functionTableCell}><code>{this._functionSignature(d)}</code></td>
            <td>{d.description}</td>
          </tr>
          {details}
        </tbody>
      );
    });
  }

  _onPageChange = (newPage, pageSize) => {
    this.setState({ currentPage: newPage, pageSize: pageSize });
  }

  _filterDescriptors = (filter) => {
    const { currentPage, pageBeforeFilter } = this.state;
    const { functionDescriptors } = this.props;

    if (!functionDescriptors) {
      return;
    }

    if (filter.length <= 0) {
      this.setState({
        filteredDescriptors: functionDescriptors,
        currentPage: pageBeforeFilter || 1,
        pageBeforeFilter: undefined,
      });

      return;
    }

    const filteredDescriptiors = functionDescriptors.filter((descriptor) => {
      const regexp = RegExp(filter);

      return regexp.test(this._functionSignature(descriptor)) || regexp.test(descriptor.description);
    });

    this.setState({
      filteredDescriptors: filteredDescriptiors,
      pageBeforeFilter: this.pageBeforeFilter || currentPage,
      currentPage: 1,
    });
  }

  _onFilterReset = () => {
    const { pageBeforeFilter } = this.state;
    const { functionDescriptors } = this.props;

    this.setState({
      filteredDescriptors: functionDescriptors,
      currentPage: pageBeforeFilter || 1,
      pageBeforeFilter: undefined,
    });
  }

  render() {
    const { currentPage, filteredDescriptors, pageSize } = this.state;
    const { functionDescriptors } = this.props;

    if (!functionDescriptors) {
      return <Spinner />;
    }

    const ruleDescriptors = filteredDescriptors || functionDescriptors;
    const pagedEntries = ruleDescriptors.slice((currentPage - 1) * pageSize, currentPage * pageSize);

    return (
      <Panel header="Rules quick reference">
        <Row className="row-sm">
          <Col md={12}>
            <p className={RuleHelperStyle.marginQuickReferenceText}>
              Read the <DocumentationLink page={DocsHelper.PAGES.PIPELINE_RULES}
                                          text="full documentation" />{' '}
              to gain a better understanding of how Graylog pipeline rules work.
            </p>
          </Col>
        </Row>
        <Row className="row-sm">
          <Col md={12}>
            <Tabs id="functionsHelper" defaultActiveKey={1} animation={false}>
              <Tab eventKey={1} title="Functions">
                <Row>
                  <Col sm={12}>
                    <p className={RuleHelperStyle.marginTab}>
                      This is a list of all available functions in pipeline rules. Click on a row to see more information
                      about the function parameters.
                    </p>
                  </Col>
                </Row>
                <Row>
                  <Col sm={12}>
                    <SearchForm onSearch={this._filterDescriptors}
                                label="Filter rules"
                                topMargin={0}
                                searchButtonLabel="Filter"
                                onReset={this._onFilterReset} />
                    <div className={`table-responsive ${RuleHelperStyle.marginTab}`}>
                      <PaginatedList totalItems={ruleDescriptors.length}
                                     pageSize={pageSize}
                                     onChange={this._onPageChange}
                                     activePage={currentPage}
                                     showPageSizeSelect={false}>
                        <Table condensed>
                          <thead>
                            <tr>
                              <th>Function</th>
                              <th>Description</th>
                            </tr>
                          </thead>
                          {this._renderFunctions(pagedEntries)}
                        </Table>
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
        </Row>
      </Panel>
    );
  }
}

RuleHelper.propTypes = {
  functionDescriptors: PropTypes.array,
};

RuleHelper.defaultProps = {
  functionDescriptors: undefined,
};

export default connect(RuleHelper,
  { ruleStore: RulesStore },
  ({ ruleStore }) => ({ ...ruleStore }));
