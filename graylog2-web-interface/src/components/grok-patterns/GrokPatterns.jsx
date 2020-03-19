import React from 'react';
import createReactClass from 'create-react-class';
import styled from 'styled-components';

import { Row, Col, Button } from 'components/graylog';
import PageHeader from 'components/common/PageHeader';
import EditPatternModal from 'components/grok-patterns/EditPatternModal';
import BulkLoadPatternModal from 'components/grok-patterns/BulkLoadPatternModal';
import DataTable from 'components/common/DataTable';
import IfPermitted from 'components/common/IfPermitted';
import StoreProvider from 'injection/StoreProvider';

const GrokPatternsStore = StoreProvider.getStore('GrokPatterns');

const GrokPatternsList = styled(DataTable)`
  th.name {
    min-width: 200px;
  }

  td {
    word-break: break-all;
  }
`;

const GrokPatterns = createReactClass({
  displayName: 'GrokPatterns',

  getInitialState() {
    return {
      patterns: [],
    };
  },

  componentDidMount() {
    this.loadData();
  },

  componentWillUnmount() {
    if (this.loadPromise) {
      this.loadPromise.cancel();
    }
  },

  loadData() {
    this.loadPromise = GrokPatternsStore.loadPatterns((patterns) => {
      if (!this.loadPromise.isCancelled()) {
        this.loadPromise = undefined;
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

  testPattern(pattern, callback, errCallback) {
    GrokPatternsStore.testPattern(pattern, callback, errCallback);
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
    const patterns = this.state.patterns.filter(p => p.name !== pattern.name);
    return (
      <tr key={pattern.id}>
        <td>{pattern.name}</td>
        <td>{pattern.pattern}</td>
        <td>
          <IfPermitted permissions="inputs:edit">
            <Button style={{ marginRight: 5 }}
                    bsStyle="primary"
                    bsSize="xs"
                    onClick={() => this.confirmedRemove(pattern)}>
              Delete
            </Button>
            <EditPatternModal id={pattern.id}
                              name={pattern.name}
                              pattern={pattern.pattern}
                              testPattern={this.testPattern}
                              patterns={patterns}
                              create={false}
                              reload={this.loadData}
                              savePattern={this.savePattern}
                              validPatternName={this.validPatternName} />
          </IfPermitted>
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
          <IfPermitted permissions="inputs:edit">
            <span>
              <BulkLoadPatternModal onSuccess={this.loadData} />
              <EditPatternModal id=""
                                name=""
                                pattern=""
                                patterns={this.state.patterns}
                                create
                                testPattern={this.testPattern}
                                reload={this.loadData}
                                savePattern={this.savePattern}
                                validPatternName={this.validPatternName} />
            </span>
          </IfPermitted>
        </PageHeader>

        <Row className="content">
          <Col md={12}>
            <IfPermitted permissions="inputs:read">
              <GrokPatternsList id="grok-pattern-list"
                                className="table-striped table-hover"
                                headers={headers}
                                headerCellFormatter={this._headerCellFormatter}
                                sortByKey="name"
                                rows={this.state.patterns}
                                dataRowFormatter={this._patternFormatter}
                                filterLabel="Filter patterns"
                                filterKeys={filterKeys} />
            </IfPermitted>
          </Col>
        </Row>
      </div>
    );
  },
});

export default GrokPatterns;
