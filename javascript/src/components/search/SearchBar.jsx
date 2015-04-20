/* global jsRoutes */

'use strict';

var React = require('react');

var SearchBar = React.createClass({
    getInitialState() {
        return {
            rangeType: null
        };
    },

    render() {
        this.props.streamId = null;
        this.props.searchQuery = "";

        var relative = (
            <select id="relative-timerange-selector" name="relative">
                <option value="300">Search in the last 5 minutes</option>
                <option value="900">Search in the last 15 minutes</option>
                <option value="1800">Search in the last 30 minutes</option>
                <option value="3600">Search in the last 1 hour</option>
                <option value="7200">Search in the last 2 hours</option>
                <option value="28800">Search in the last 8 hours</option>
                <option value="86400">Search in the last 1 day</option>
                <option value="172800">Search in the last 2 days</option>
                <option value="432000">Search in the last 5 days</option>
                <option value="604800">Search in the last 7 days</option>
                <option value="1209600">Search in the last 14 days</option>
                <option value="2592000">Search in the last 30 days</option>
                <option value="0">Search in all messages</option>
            </select>
        );

        return (
            <div className="row no-bm">
                <div className="col-md-12" id="universalsearch-container">
                    <div className="row no-bm">
                        <div className="col-md-12" id="universalsearch">
                            <form className="universalsearch-form"
                                  action={this.props.streamId ?  "unimplemented" : jsRoutes.controllers.SearchControllerV2.index().url }
                                  method="GET">
                                <input type="hidden" name="rangetype"
                                       value="relative"
                                       id="universalsearch-rangetype"/>
                                <input type="hidden"
                                       name="fields"
                                       id="universalsearch-fields"
                                       value=""/>
                                <input type="hidden"
                                       name="width"
                                       value="-1"/>

                                {this.state.rangeType && <span id="universalsearch-rangetype-permanent"
                                                               style={{display: "none"}}>{this.state.rangeType}</span>}

                                <span id="universalsearch-query-permanent"
                                      style={{display: 'none'}}>{this.props.searchQuery}</span>

                                <span id="universalsearch-interval-permanent"
                                      style={{display: 'none'}}>{this.state.interval}</span>

                                <div className="timerange-selector-container" style={{float: 'left'}}>
                                    <div className="btn-group timerange-chooser">
                                        <a className="btn btn-info dropdown-toggle" data-toggle="dropdown" href="#">
                                            <i className="fa fa-clock-o"></i>
                                            <span className="caret"></span>
                                        </a>
                                        <ul className="dropdown-menu">
                                            <li><a href="#" data-selector-name="relative"
                                                   className="selected">Relative</a></li>
                                            <li><a href="#" data-selector-name="absolute">Absolute</a></li>
                                            <li><a href="#" data-selector-name="keyword">Keyword</a></li>
                                        </ul>
                                    </div>

                                    <div className="timerange-selector relative" style={{width: 270, display: "block"}}>
                                        {relative}
                                    </div>

                                    <div className="timerange-selector absolute" style={{width: 570, display: "none"}}>
                                        @partials.timerangeselectors.absolute()
                                    </div>

                                    <div className="timerange-selector keyword" style={{width: 353, display: "none"}}>
                                        @partials.timerangeselectors.keyword()
                                    </div>
                                </div>

                                <br style={{clear: 'both'}}/>

                                <div id="search-container">
                                    <button type="submit" className="btn btn-success pull-left"><i
                                        className="fa fa-search"></i></button>
                                    <div className="query">
                                        <input type="text" id="universalsearch-query" name="q"
                                               value={this.props.searchQuery}
                                               placeholder='Type your search query here and press enter. ("not found" AND http) OR http_response_code:[400 TO 404]'/>
                                    </div>
                                </div>
                            </form>
                        </div>
                    </div>
                </div>
            </div>
        );
    }
});

module.exports = SearchBar;