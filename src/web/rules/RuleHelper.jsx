import React from 'react';
import { Row, Col, Panel, Table, Tabs, Tab } from 'react-bootstrap';

import Reflux from 'reflux';

import RulesStore from './RulesStore';
import RulesActions from './RulesActions';
import ObjectUtils from 'util/ObjectUtils';

import DocumentationLink from 'components/support/DocumentationLink';
import { PaginatedList, Spinner } from 'components/common';

import DocsHelper from 'util/DocsHelper';

const RuleHelper = React.createClass({
  mixins: [
    Reflux.connect(RulesStore),
  ],

  getInitialState() {
    return {
      expanded: {},
      paginatedEntries: undefined,
      filteredEntries: undefined,
      currentPage: 1,
      pageSize: 10,
    };
  },

  componentDidMount() {
    RulesActions.loadFunctions();
  },

  ruleTemplate: `rule "function howto"
when
  has_field("transaction_date")
then
  // the following date format assumes there's no time zone in the string
  let new_date = parse_date(to_string($message.transaction_date), "yyyy-MM-dd HH:mm:ss");
  set_field("transaction_year", new_date.year);
end`,

  _niceType(typeName) {
    return typeName.replace(/^.*\.(.*?)$/, '$1');
  },

  _toggleFunctionDetail(functionName) {
    const newState = ObjectUtils.clone(this.state.expanded);
    newState[functionName] = !newState[functionName];
    this.setState({ expanded: newState });
  },

  _functionSignature(descriptor) {
    const args = descriptor.params.map(p => { return p.optional ? `[${p.name}]` : p.name; });
    return <code>{`${descriptor.name}(${args.join(', ')}) : ${this._niceType(descriptor.return_type)}`}</code>;
  },

  _parameters(descriptor) {
    return descriptor.params.map(p => {
      return (
        <tr key={p.name}>
          <td style={{ width: '1%' }}>{p.name}</td>
          <td style={{ width: '1%' }}>{this._niceType(p.type)}</td>
          <td style={{ width: '1%', textAlign: 'center' }}>{p.optional ? null : <i className="fa fa-check"/>}</td>
          <td>{p.description}</td>
        </tr>);
    });
  },

  _renderFunctions(descriptors) {
    if (!descriptors) return [];
    return descriptors.map((d) => {
      let details = null;
      if (this.state.expanded[d.name]) {
        details = (<tr>
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
        </tr>);
      }
      return (<tbody key={d.name} onClick={() => this._toggleFunctionDetail(d.name)} style={{ cursor: 'pointer' }}>
        <tr>
          <td style={{ width: 300 }}>{this._functionSignature(d)}</td>
          <td>{d.description}</td>
        </tr>
        {details}
      </tbody>);
    });
  },

  _onPageChange(newPage, pageSize) {
    this.setState({ currentPage: newPage, pageSize: pageSize });
  },

  render() {
    if (!this.state.functionDescriptors) {
      return <Spinner />;
    }

    const pagedEntries = this.state.functionDescriptors.slice((this.state.currentPage - 1) * this.state.pageSize, (this.state.currentPage * this.state.pageSize) - 1);

    return (
      <Panel header="Rules quick reference">
        <Row className="row-sm">
          <Col md={12}>
            <p style={{ marginTop: 5 }}>
              Read the <DocumentationLink page={DocsHelper.PAGES.PIPELINE_RULES}
                                                 text="full documentation" />{' '}
              to gain a better understanding of how Graylog pipeline rules work.
            </p>
          </Col>
        </Row>
        <Row className="row-sm">
          <Col md={12}>
            <Tabs defaultActiveKey={1} animation={false}>
              <Tab eventKey={1} title="Functions">
                <div className="table-responsive" style={{ marginTop: 10 }}>
                  <PaginatedList totalItems={this.state.functionDescriptors.length} pageSize={this.state.pageSize} pageSizes={[10]} onChange={this._onPageChange}>
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
                <p>See all functions in the <DocumentationLink page={DocsHelper.PAGES.PIPELINE_FUNCTIONS}
                                                               text="documentation" />.</p>
              </Tab>
              <Tab eventKey={2} title="Example">
                <pre style={{ marginTop: 10, whiteSpace: 'pre-wrap' }}>
                  {this.ruleTemplate}
                </pre>
              </Tab>
            </Tabs>
          </Col>
        </Row>
      </Panel>
    );
  },
});

export default RuleHelper;
