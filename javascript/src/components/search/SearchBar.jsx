/* global jsRoutes, momentHelper */

'use strict';

var $ = require('jquery');

var React = require('react');
var Input = require('react-bootstrap').Input;
var Button = require('react-bootstrap').Button;
var ButtonToolbar = require('react-bootstrap').ButtonToolbar;
var DropdownButton = require('react-bootstrap').DropdownButton;
var MenuItem = require('react-bootstrap').MenuItem;

var SearchStore = require('../../stores/search/SearchStore');

var SearchBar = React.createClass({
    getInitialState() {
        var searchParams = SearchStore.getParams();
        return {
            rangeType: searchParams.rangeType,
            rangeParams: searchParams.rangeParams,
            query: searchParams.query,
            streamId: null
        };
    },
    componentDidMount() {
        SearchStore.onParamsChanged = (newParams) => this.setState(newParams);

        this.universalSearchElement = React.findDOMNode(this.refs.universalSearch);
        $(this.universalSearchElement).on('get-original-search.graylog.universalsearch', this._getOriginalSearchRequest);
    },
    componentWillUnmount() {
        $(this.universalSearchElement).off('get-original-search.graylog.universalsearch', this._getOriginalSearchRequest);
    },
    _getOriginalSearchRequest(event, data) {
        data.callback(SearchStore.getSearchURLParams());
    },
    _queryChanged() {
        SearchStore.query = this.refs.query.getValue();
    },
    _rangeTypeChanged(newRangeType) {
        SearchStore.rangeType = newRangeType;
    },
    _rangeParamsChanged(key) {
        return () => {
            this.setState({rangeParams: this.state.rangeParams.set(key, this.refs[key].getValue())});
        };
    },
    _getRangeTypeSelector() {
        var selector;

        switch (this.state.rangeType) {
            case 'relative':
                selector = (
                    <div className="timerange-selector relative"
                         style={{width: 270, marginLeft: 50}}>
                        <Input id='relative-timerange-selector'
                               ref='relative'
                               type='select'
                               value={this.state.rangeParams.get('relative')}
                               name='relative'
                               onChange={this._rangeParamsChanged('relative')}
                               className='input-sm'>
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
                        </Input>
                    </div>
                );
                break;
            case 'absolute':
                var setToNowButton = <Button bsSize='small'><i className="fa fa-magic"></i></Button>;
                selector = (
                    <div className="timerange-selector absolute" style={{width: 570}}>
                        <div className='row no-bm' style={{marginLeft: 50}}>
                            <div className='col-md-5' style={{padding: 0}}>
                                <Input type='text'
                                       ref='from'
                                       name='from'
                                       value={this.state.rangeParams.get('from')}
                                       onChange={this._rangeParamsChanged('from')}
                                       placeholder={momentHelper.DATE_FORMAT}
                                       buttonAfter={setToNowButton}
                                       bsSize='small'/>
                            </div>
                            <div className='col-md-1'>
                                <p className='text-center' style={{margin: 0, lineHeight: '30px'}}>to</p>
                            </div>
                            <div className='col-md-5' style={{padding: 0}}>
                                <Input type='text'
                                       ref='to'
                                       name='to'
                                       value={this.state.rangeParams.get('to')}
                                       onChange={this._rangeParamsChanged('to')}
                                       placeholder={momentHelper.DATE_FORMAT}
                                       buttonAfter={setToNowButton}
                                       bsSize='small'/>
                            </div>
                        </div>
                    </div>
                );
                break;
            case 'keyword':
                selector = (
                    <div className="timerange-selector keyword" style={{width: 353, marginLeft: 50}}>
                        <Input type='text'
                               ref='keyword'
                               name='keyword'
                               value={this.state.rangeParams.get('keyword')}
                               onChange={this._rangeParamsChanged('keyword')}
                               placeholder='Last week'
                               className='input-sm'/>
                    </div>
                );
                break;
            default:
                throw('Unsupported range type ' + this.state.rangeType);
        }

        return selector;
    },

    render() {
        return (
            <div className="row no-bm">
                <div className="col-md-12" id="universalsearch-container">
                    <div className="row no-bm">
                        <div ref="universalSearch" className="col-md-12" id="universalsearch">
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

                                <div className="timerange-selector-container">
                                    <ButtonToolbar className='timerange-chooser pull-left'>
                                        <DropdownButton bsStyle='info'
                                                        title={<i className="fa fa-clock-o"></i>}
                                                        onSelect={this._rangeTypeChanged}>
                                            <MenuItem eventKey='relative'
                                                      className={this.state.rangeType === 'relative' ? 'selected' : null}>
                                                Relative
                                            </MenuItem>
                                            <MenuItem eventKey='absolute'
                                                      className={this.state.rangeType === 'absolute' ? 'selected' : null}>
                                                Absolute
                                            </MenuItem>
                                            <MenuItem eventKey='keyword'
                                                      className={this.state.rangeType === 'keyword' ? 'selected' : null}>
                                                Keyword
                                            </MenuItem>
                                        </DropdownButton>
                                    </ButtonToolbar>

                                    {this._getRangeTypeSelector()}
                                </div>

                                <div id="search-container">
                                    <Button type='submit' bsStyle='success' className='pull-left'>
                                        <i className="fa fa-search"></i>
                                    </Button>

                                    <div className="query">
                                        <Input type='text'
                                               ref='query'
                                               name='q'
                                               value={this.state.query}
                                               onChange={this._queryChanged}
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