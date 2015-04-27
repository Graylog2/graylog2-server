'use strict';

var React = require('react/addons');
var StreamsStore = require('../../stores/streams/StreamsStore');
var StreamList = require('./StreamList');

var StreamComponent = React.createClass({
    getInitialState() {
        return {
            streams: []
        };
    },
    componentDidMount() {
        this.loadData();
    },
    loadData() {
        StreamsStore.load((streams) => {
            this.setState({streams: streams});
        });
    },
    _onDeleteStream(stream) {
        if (window.confirm("Do you really want to remove this stream?")) {
            StreamsStore.remove(stream.id, () => {
                this.loadData();
            });
        }
    },
    render() {
        return (
            <StreamList streams={this.state.streams} onDelete={this._onDeleteStream}/>
        );
    }
});

module.exports = StreamComponent;
