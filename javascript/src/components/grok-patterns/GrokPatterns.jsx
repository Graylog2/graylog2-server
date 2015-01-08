'use strict';

var React = require('react');
var URLUtils = require("../../util/URLUtils");
var EditPatternModal = require('./EditPatternModal');
var $ = require('jquery'); // excluded and shimed

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
        $.get(URLUtils.appPrefixed('/a/system/grokpatterns'), (result) => {
            if (this.isMounted()) {
                this.setState({
                    patterns: result
                });
            }
        });
    },
    _getSortedFilteredPatterns() {
        var patterns = this.state.patterns;
        patterns.sort((pattern1, pattern2) => {
            return pattern1.name.toLowerCase().localeCompare(pattern2.name.toLowerCase());
        });
        var filter = this.state.filter;
        return patterns.filter((pattern) => { return pattern.name.toLowerCase().indexOf(filter) !== -1 });
    },
    
    _sortedFilteredPatternsHtml() {
        var patterns = this._getSortedFilteredPatterns();
        var jsx = patterns.map((pattern) => {
            return (
                <tr key={pattern.id}>
                    <td>{pattern.name}</td>
                    <td>{pattern.pattern}</td>
                    <td><EditPatternModal id={pattern.id} name={pattern.name} pattern={pattern.pattern} reload={this.loadData}/></td>
                </tr>
            );
        }, this);

        return (
            <table className="table table-striped">
                <thead>
                    <tr>
                        <th>Name</th>
                        <th>Pattern</th>
                        <th></th>
                    </tr>
                </thead>
                <tbody>
                    {jsx}
                </tbody>
            </table>);
    },
    
    _updateFilter(event) {
        var filter = event.target.value;
        this.setState({filter: filter});
    },
    
    render() {
        return (
            <div>
                <div className="row-fluid">
                    <div className="span4">
                        <label for="filter">Search for pattern names:</label>
                        <input type="text" name="filter" value={this.state.filter} onChange={this._updateFilter}/>
                    </div>
                    <div className="pull-right">
                        <EditPatternModal id={""} name={""} pattern={""} reload={this.loadData} />
                    </div>
                </div>
                <div className="grok-patterns row-fluid">
                {this._sortedFilteredPatternsHtml()}
                </div>
            </div>
        );
    }
});

module.exports = GrokPatterns;
