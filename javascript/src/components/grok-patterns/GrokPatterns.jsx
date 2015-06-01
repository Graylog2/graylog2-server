'use strict';

var React = require('react');

var EditPatternModal = require('./EditPatternModal');
var BulkLoadPatternModal = require('./BulkLoadPatternModal');
var DataTable = require('../common/DataTable');
var GrokPatternsStore = require('../../stores/grok-patterns/GrokPatternsStore');

var GrokPatterns = React.createClass({
    getInitialState() {
        return {
            patterns: []
        };
    },
    componentDidMount() {
        this.loadData();
    },
    loadData() {
        GrokPatternsStore.loadPatterns((patterns) => {
            if (this.isMounted()) {
                this.setState({
                    patterns: patterns
                });
            }
        });
    },
    validPatternName(name) {
        // Check if patterns already contain a pattern with the given name.
        return !this.state.patterns.some((p) => p.name === name);
    },
    savePattern(pattern, callback) {
        GrokPatternsStore.savePattern(pattern, () => {
            callback();
            this.loadData();
        });
    },
    confirmedRemove(pattern) {
        if (window.confirm("Really delete the grok pattern " + pattern.name + "?\nIt will be removed from the system and unavailable for any extractor. If it is still in use by extractors those will fail to work.")) {
            GrokPatternsStore.deletePattern(pattern, this.loadData);
        }
    },
    _headerCellFormatter(header) {
        var formattedHeaderCell;

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
                    <button style={{marginRight: 5}} className="btn btn-danger btn-xs"
                            onClick={this.confirmedRemove.bind(this, pattern)}>
                        <i className="fa fa-remove"></i> Delete
                    </button>
                    <EditPatternModal id={pattern.id} name={pattern.name} pattern={pattern.pattern} create={false}
                                      reload={this.loadData} savePattern={this.savePattern}
                                      validPatternName={this.validPatternName}/>
                </td>
            </tr>
        );
    },
    render() {
        var headers = ["Name", "Pattern", "Actions"];
        var filterKeys = ["name"];

        return (
            <div>
                <div className="row content content-head">
                    <div className="col-md-12">
                        <div className="pull-right actions">
                            <BulkLoadPatternModal />
                            <EditPatternModal id={""} name={""} pattern={""} create={true}
                                              reload={this.loadData}
                                              savePattern={this.savePattern}
                                              validPatternName={this.validPatternName}/>
                        </div>

                        <h1>Grok patterns</h1>

                        <p className="description">
                            This is a list of grok patterns you can use in your Graylog grok extractors. You can add
                            your own manually or import a whole list of patterns from a so called pattern file.
                        </p>
                    </div>
                </div>

                <div className="row content">
                    <div className="col-md-12">
                        <DataTable id="grok-pattern-list"
                                   className="table-striped table-hover"
                                   headers={headers}
                                   headerCellFormatter={this._headerCellFormatter}
                                   sortByKey={"name"}
                                   rows={this.state.patterns}
                                   dataRowFormatter={this._patternFormatter}
                                   filterLabel="Filter patterns"
                                   filterKeys={filterKeys}/>
                    </div>
                </div>
            </div>
        );
    }
});

module.exports = GrokPatterns;
