'use strict';

var React = require('react');
//noinspection JSUnusedGlobalSymbols
var EditPatternModal = require('./EditPatternModal');
var GrokPatternsStore = require('../../stores/grok-patterns/GrokPatternsStore');

var GrokPatterns = React.createClass({
    getInitialState() {
        return {
            patterns: [],
            filter: ""
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
    _getFilteredPatterns() {
        var filter = this.state.filter.toLowerCase().trim();
        return this.state.patterns.filter((pattern) => { return pattern.name.toLowerCase().indexOf(filter) !== -1; });
    },
    
    _filteredPatternsHtml() {
        var patterns = this._getFilteredPatterns();
        var jsx = patterns.map((pattern) => {
            return (
                <tr key={pattern.id}>
                    <td>{pattern.name}</td>
                    <td>{pattern.pattern}</td>
                    <td>
                        <button style={{marginRight: 5}} className="btn btn-mini" onClick={this.confirmedRemove.bind(this, pattern)}>
                            <i className="icon icon-remove"></i> Delete
                        </button>
                        <EditPatternModal id={pattern.id} name={pattern.name} pattern={pattern.pattern} create={false} reload={this.loadData} savePattern={this.savePattern}/>
                    </td>
                </tr>
            );
        }, this);

        return (
            <table className="table table-striped">
                <thead>
                    <tr>
                        <th>Name</th>
                        <th>Pattern</th>
                        <th style={{width: 180}}>Actions</th>
                    </tr>
                </thead>
                <tbody>
                    {jsx}
                </tbody>
            </table>);
    },
    savePattern(pattern, callback) {
        GrokPatternsStore.savePattern(pattern, () => {
            callback();
            this.loadData();
        });
    },
    confirmedRemove(pattern) {
        if (window.confirm("Really delete the grok pattern " + pattern.name + "?\nIt will be removed from the system and unavailable for any extractor. If it is still in uses extractors will fail to work.")) {
            GrokPatternsStore.deletePattern(pattern, this.loadData);
        }
    },

    render() {
        return (
            <div>
                <div className="row-fluid">
                    <div className="span4">
                        <label htmlFor="grokfilter">Search for pattern names:</label>
                        <input type="text" name="filter" id="grokfilter" value={this.state.filter} onChange={(event) => {this.setState({filter: event.target.value});}} />
                    </div>
                    <div className="pull-right">
                        <EditPatternModal id={""} name={""} pattern={""} create={true} reload={this.loadData} savePattern={this.savePattern} />
                    </div>
                </div>
                <div className="grok-patterns row-fluid">
                {this._filteredPatternsHtml()}
                </div>
            </div>
        );
    }
});

module.exports = GrokPatterns;
