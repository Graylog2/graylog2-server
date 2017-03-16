import React from 'react';
import { Row, Col, Button } from 'react-bootstrap';

import StoreProvider from 'injection/StoreProvider';
const GrokPatternsStore = StoreProvider.getStore('GrokPatterns');

import PageHeader from 'components/common/PageHeader';
import EditPatternModal from 'components/grok-patterns/EditPatternModal';
import BulkLoadPatternModal from 'components/grok-patterns/BulkLoadPatternModal';
import DataTable from 'components/common/DataTable';

const GrokPatterns = React.createClass({
  getInitialState() {
    return {
      patterns: [],
    };
  },
  componentDidMount() {
    this.loadData();
  },
  loadData() {
    GrokPatternsStore.loadPatterns((patterns) => {
      if (this.isMounted()) {
        this.setState({
          patterns: patterns,
        });
      }
    });
  },
  validPatternName(name) {
    // Check if patterns already contain a pattern with the given name.
    return !this.state.patterns.some(pattern => pattern.name === name);
  },
  savePattern(pattern, callback) {
    GrokPatternsStore.savePattern(pattern, () => {
      callback();
      this.loadData();
    });
  },
  confirmedRemove(pattern) {
    if (window.confirm(`Really delete the grok pattern ${pattern.name}?\nIt will be removed from the system and unavailable for any extractor. If it is still in use by extractors those will fail to work.`)) {
      GrokPatternsStore.deletePattern(pattern, this.loadData);
    }
  },
  _headerCellFormatter(header) {
    let formattedHeaderCell;

    switch (header.toLocaleLowerCase()) {
      case 'name':
        formattedHeaderCell = <th className="name">{header}</th>;
        break;
      case 'actions':
        formattedHeaderCell = <th className="actions">{header}</th>;
        break;
      default:
        formattedHeaderCell = <th>{header}</th>;
    }

    return formattedHeaderCell;
  },
  _patternFormatter(pattern) {
    return (
      <tr key={pattern.id}>
        <td>{pattern.name}</td>
        <td>{pattern.pattern}</td>
        <td>
          <Button style={{ marginRight: 5 }} bsStyle="primary" bsSize="xs"
                  onClick={this.confirmedRemove.bind(this, pattern)}>
            Delete
          </Button>
          <EditPatternModal id={pattern.id} name={pattern.name} pattern={pattern.pattern} create={false}
                            reload={this.loadData} savePattern={this.savePattern}
                            validPatternName={this.validPatternName} />
        </td>
      </tr>
    );
  },
  render() {
    const headers = ['Name', 'Pattern', 'Actions'];
    const filterKeys = ['name'];

    return (
      <div>
        <PageHeader title="Grok patterns">
          <span>
            This is a list of grok patterns you can use in your Graylog grok extractors. You can add
            your own manually or import a whole list of patterns from a so called pattern file.
          </span>
          {null}
          <span>
            <BulkLoadPatternModal onSuccess={this.loadData} />
            <EditPatternModal id={''} name={''} pattern={''} create
                              reload={this.loadData}
                              savePattern={this.savePattern}
                              validPatternName={this.validPatternName} />
          </span>
        </PageHeader>

        <Row className="content">
          <Col md={12}>
            <DataTable id="grok-pattern-list"
                       className="table-striped table-hover"
                       headers={headers}
                       headerCellFormatter={this._headerCellFormatter}
                       sortByKey={'name'}
                       rows={this.state.patterns}
                       dataRowFormatter={this._patternFormatter}
                       filterLabel="Filter patterns"
                       filterKeys={filterKeys} />
          </Col>
        </Row>
      </div>
    );
  },
});

export default GrokPatterns;
