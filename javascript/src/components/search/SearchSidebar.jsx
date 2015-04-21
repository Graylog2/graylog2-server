'use strict';

var React = require('react');
var Modal = require('react-bootstrap').Modal;
var ModalTrigger = require('react-bootstrap').ModalTrigger;

var numeral = require('numeral');
var Immutable = require('immutable');

var MessageField = React.createClass({
    render() {
        return (
            <li className="search-result-field-type">
                <i className="fa fa-fw open-analyze-field fa-caret-right"></i>
                <input type="checkbox"
                       id={"field-selector-" + this.props.field.hash}
                       className="field-selector"
                       checked={this.props.selected}
                       onChange={(event) => this.props.onToggled(this.props.field.name)}
                    />
                <label htmlFor={"field-selector-" + this.props.field.hash} className="field-name">{this.props.field.name}</label>
            </li>
        );
    }
});

var SearchSidebar = React.createClass({
    render() {
        var indicesModal =
            <Modal title='Used Indices' onRequestHide={() => {}}>
                <div className="modal-body">
                    <p>Graylog is intelligently selecting the indices it needs to search upon based on the time frame
                        you selected.
                        This list of indices is mainly useful for debugging purposes.</p>
                    <h4>Indices used for this search:</h4>

                    <ul className="index-list">
                        {this.props.result['used_indices'].map((index) => <li key={index.index}> {index.index}</li>)}
                    </ul>
                </div>
            </Modal>;

        return (
            <div className="content-col">
                <h3>Found {numeral(this.props.result['total_result_count']).format("0,0")} messages</h3>

                <p style={{marginTop: 3}}>
                    Search took {numeral(this.props.result['took_ms']).format("0,0")} ms, searched in <ModalTrigger
                    modal={indicesModal}><a href="#">{this.props.result['used_indices'].length}&nbsp;{this.props.result['used_indices'].length === 1 ? "index" : "indices"}</a></ModalTrigger>.
                </p>

                <div style={{marginTop: 10}}>
                    <span>TODO Missing stream and dashboard thingies</span>
                    <a href="#" className="btn btn-success btn-sm">Save search criteria</a>
                </div>

                <hr />

                <h1 style={{display: 'inline-block'}}>Fields</h1>

                <ul className="search-result-fields">
                    {this.props.result['page_fields']
                        .sort((a,b) => a.name.localeCompare(b.name))
                        .map((field) => <MessageField key={field.name} field={field} onToggled={this.props.onFieldToggled} selected={this.props.selectedFields.contains(field.name)}/>)}
                </ul>

            </div>
        );
    }
});

module.exports = SearchSidebar;