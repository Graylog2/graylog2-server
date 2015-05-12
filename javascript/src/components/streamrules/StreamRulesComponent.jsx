'use strict';

var React = require('react/addons');
var StreamRuleList = require('./StreamRuleList');
var StreamsStore = require('../../stores/streams/StreamsStore');

var StreamRulesComponent = React.createClass({
    getInitialState() {
        return {
        };
    },
    loadData(props) {
        StreamsStore.get(props.streamId, (stream) => {
            this.setState({stream: stream});
        });
    },
    _onSubmit(streamRuleId, data) {
        this.loadData(this.props);
        if (this.props.onSubmit) {
            this.props.onSubmit(streamRuleId, data);
        }
    },
    componentDidMount() {
        this.loadData(this.props);
    },
    willReceiveProps(props) {
        this.loadData(props);
    },
    _getListClassName() {
        return "alert " + (this.props.matchData.matches ? "alert-success" : "alert-danger");
    },
    render() {
        if (this.state.stream) {
            var alertClassName = (this.props.matchData ? this._getListClassName() : "alert alert-info");
            return (
                <div className={alertClassName}>
                    <StreamRuleList permissions={this.props.permissions} stream={this.state.stream}
                                    streamRuleTypes={this.props.streamRuleTypes}
                                    matchData={this.props.matchData} onSubmit={this._onSubmit}/>
                </div>
            );
        } else {
            return (
                <div className="alarm-callback-component">
                    <i className="fa fa-spin fa-spinner"/> Loading
                </div>
            );
        }
    }
});

module.exports = StreamRulesComponent;
