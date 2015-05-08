'use strict';

var React = require('react/addons');
var LoaderTabs = require('../messageloaders/LoaderTabs');
var StreamRulesComponent = require('./StreamRulesComponent');
var StreamsStore = require('../../stores/streams/StreamsStore');

var StreamRulesEditor = React.createClass({
    getInitialState() {
        return {};
    },
    onMessageLoaded(message) {
        this.setState({message: message});
        StreamsStore.testMatch(this.props.streamId, {message: message.fields}, (resultData) => {
            this.setState({matchData: resultData});
        });
    },
    onStreamRuleFormSubmit() {
        this.onMessageLoaded(this.state.message);
    },
    render() {
        return (
            <div className="row content">
                <div className="col-md-12 streamrule-sample-message">
                    <h2>
                        1. Load a message to test rules
                    </h2>
    
                    <LoaderTabs onMessageLoaded={this.onMessageLoaded}/>
    
                    <div className="spinner" style={{display: "none"}}><h2><i className='fa fa-spinner fa-spin'></i> &nbsp;Loading message</h2></div>
    
                    <div className="sample-message-display" style={{display: "none", marginTop: "5px"}}>
                        <strong>Next step:</strong>
                        Add/delete/modify stream rules in step 2 and see if the example message would have been routed into the stream or not.
                    </div>
    
                    <hr />
    
                    <div className="buttons pull-right">
                        <button className="btn btn-success btn-sm show-stream-rule" data-stream-id="@stream.getId">
                            Add stream rule
                        </button>
                    </div>
    
                    <h2>
                        <i className="fa fa-cogs"></i>
                        2. Manage stream rules
                    </h2>
    
                    The example message would have been routed into this stream if every rule below has a green background.
    
                    <StreamRulesComponent streamId={this.props.streamId} permissions={this.props.permissions}
                                          matchData={this.state.matchData} onSubmit={this.onStreamRuleFormSubmit}/>
    
                    <p style={{marginTop: '10px'}}>
                        <a href={jsRoutes.controllers.StreamsController.index().url} className="btn btn-success">I'm done!</a>
                    </p>
                </div>
            </div>
        );
    }
});

module.exports = StreamRulesEditor;
