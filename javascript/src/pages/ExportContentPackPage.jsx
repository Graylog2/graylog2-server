import React from 'react';
import { Row, Col, Button } from 'react-bootstrap';

import UserNotification from 'util/UserNotification';

import DashboardsStore from 'stores/dashboards/DashboardsStore';
import GrokPatternsStore from 'stores/grok-patterns/GrokPatternsStore';
import InputsStore from 'stores/inputs/InputsStore';
import OutputsStore from 'stores/outputs/OutputsStore';
import StreamsStore from 'stores/streams/StreamsStore';

import ConfigurationBundlesActions from 'actions/configuration-bundles/ConfigurationBundlesActions';

import PageHeader from 'components/common/PageHeader';

const ExportContentPackPage = React.createClass({
  getInitialState() {
    return {};
  },
  componentDidMount() {
    DashboardsStore.listDashboards().then((dashboards) => {
      this.setState({dashboards: dashboards});
    });
    GrokPatternsStore.loadPatterns((grokPatterns) => {
      this.setState({grok_patterns: grokPatterns});
    });
    InputsStore.list((inputs) => {
      this.setState({inputs: inputs});
    });
    OutputsStore.load((resp) => {
      this.setState({outputs: resp.outputs});
    });
    StreamsStore.listStreams().then((streams) => {
      this.setState({streams: streams});
    });
  },
  onSubmit(evt) {
    evt.preventDefault();
    const request = {
      streams: [],
      inputs: [],
      outputs: [],
      dashboards: [],
      grok_patterns: [],
    };
    Object.keys(this.refs).forEach((key) => {
      if (key.indexOf('.') === -1) {
        request[key] = this.refs[key].value;
      } else {
        if (this.refs[key].checked) {
          const group = key.split('.')[0];
          const id = key.split('.')[1];

          request[group].push(id);
        }
      }
    });
    ConfigurationBundlesActions.export.triggerPromise(request)
      .then((response) => {
        UserNotification.success('Successfully export content pack. Starting download ...', 'Success!');

        var link = document.createElement('a');
        link.download = 'content_pack.json';
        link.href = 'data:application/x-force-download;charset=utf-8,' + encodeURIComponent(response);
        link.click();
      });
  },
  isEmpty(obj) {
    return ((obj === undefined) || (typeof obj.count === 'function' ? obj.count() === 0 : obj.length === 0));
  },
  inputDetails(input) {
    let details = input.name;
    if (input.attributes.bind_address) {
      details += ' on ' + input.attributes.bind_address;
      if (input.attributes.port) {
        details += ' port ' + input.attributes.port;
      }
    }

    return details;
  },
  formatDashboard(dashboard) {
    return (
      <div className="checkbox" key={'dashboard_checkbox-' + dashboard.id}>
        <label className="checkbox"><input ref={'dashboards.' + dashboard.id} type="checkbox" name="dashboards" id={'dashboard_' + dashboard.id} value={dashboard.id}/>{dashboard.title}</label>
      </div>
    );
  },
  formatGrokPattern(grokPattern) {
    return (
      <div className="checkbox" key={'grok_pattern_checkbox-' + grokPattern.id}>
        <label className="checkbox"><input ref={'grok_patterns.' + grokPattern.id} type="checkbox" name="grokPatterns" id={'grokPattern_' + grokPattern.id} value={grokPattern.id}/>{grokPattern.name}</label>
        <span className="help-inline">Pattern: <tt>{grokPattern.pattern}</tt></span>
      </div>
    );
  },
  formatInput(input) {
    return (
      <div className="checkbox" key={'input_checkbox-' + input.input_id}>
        <label className="checkbox"><input ref={'inputs.' + input.input_id} type="checkbox" name="inputs" id={'input_' + input.input_id} value={input.input_id}/>{input.title}</label>
        <span className="help-inline">({this.inputDetails(input)})</span>
      </div>
    );
  },
  formatOutput(output) {
    return (
      <div className="checkbox" key={'output_checkbox-' + output.id}>
        <label className="checkbox"><input ref={'outputs.' + output.id} type="checkbox" name="outputs" id={'output_' + output.id} value={output.id}/>{output.title}</label>
      </div>
    );
  },
  formatStream(stream) {
    return (
      <div className="checkbox" key={'stream_checkbox-' + stream.id}>
        <label className="checkbox"><input ref={'streams.' + stream.id} type="checkbox" name="streams" id={'stream_' + stream.id} value={stream.id}/>{stream.title}</label>
      </div>
    );
  },
  selectAll(group) {
    Object.keys(this.refs).forEach((key) => {
      if (key.startsWith(group)) {
        this.refs[key].checked = true;
      }
    });
  },
  render() {
    return (
      <span>
        <PageHeader title="Create a content pack">
          <span>Export your inputs, outputs, streams and dashboards as a content pack and share it with the community or other setups.</span>
        </PageHeader>

        <Row className="content">
          <Col md={6}>
            <form className="form-horizontal build-content-pack" onSubmit={this.onSubmit}>
              <div className="form-group">
                <Col sm={2}>
                  <label className="control-label" htmlFor="name">Name</label>
                </Col>
                <Col sm={10}>
                  <input ref="name" type="text" id="name" className="input-xlarge form-control" name="name" required/>
                  <span className="help-block">The name of your configuration bundle.</span>
                </Col>
              </div>

              <div className="form-group">
                <Col sm={2}>
                  <label className="control-label" htmlFor="description">Description</label>
                </Col>
                <Col sm={10}>
                  <textarea ref="description" rows="6" id="description" name="description" className="input-xlarge form-control" required/>
                  <span className="help-block">
                    A description of what your bundle does and possible special instructions for the user.
                    You can use <a href="http://daringfireball.net/projects/markdown/syntax" target="_blank">Markdown</a> syntax.
                  </span>
                </Col>
              </div>

              <div className="form-group">
                <Col sm={2}>
                  <label className="control-label" htmlFor="category">Category</label>
                </Col>
                <Col sm={10}>
                  <input ref="category" type="text" id="category" name="category" className="input-xlarge form-control" required/>
                  <span className="help-block">A category for your bundle, e.g. Operating Systems, Firewalls or Switches.</span>
                </Col>
              </div>

              <div className="form-group">
                <Col sm={2}>
                  <label className="control-label" htmlFor="inputs">Inputs</label>
                </Col>
                <Col sm={10}>
                  {this.isEmpty(this.state.inputs) ?
                  <span className="help-block help-standalone">There are no inputs to export.</span>
                    :
                  <span>
                    <Button className="btn btn-sm btn-link select-all" onClick={this.selectAll.bind(this, 'input')}>Select all</Button>
                    {this.state.inputs.sort((i1, i2) => {return i1.title.localeCompare(i2.title); }).map(this.formatInput)}
                  </span>
                  }
                </Col>
              </div>

              <div className="form-group">
                <Col sm={2}>
                  <label className="control-label" htmlFor="grokPatterns">Grok Patterns</label>
                </Col>
                <Col sm={10}>
                  {this.isEmpty(this.state.grok_patterns) ?
                  <span className="help-block help-standalone">There are no grok patterns to export.</span>
                    :
                  <span>
                    <Button className="btn btn-sm btn-link select-all" onClick={this.selectAll.bind(this, 'grok_pattern')}>Select all</Button>
                    {this.state.grok_patterns.sort((g1, g2) => {return g1.name.localeCompare(g2.name);}).map(this.formatGrokPattern)}
                  </span>
                    }
                </Col>
              </div>

              <div className="form-group">
                <Col sm={2}>
                  <label className="control-label" htmlFor="outputs">Outputs</label>
                </Col>
                <Col sm={10}>
                  {this.isEmpty(this.state.outputs) ?
                  <span className="help-block help-standalone">There are no outputs to export.</span>
                    :
                  <span>
                    <Button className="btn btn-sm btn-link select-all" onClick={this.selectAll.bind(this, 'output')}>Select all</Button>
                    {this.state.outputs.sort((o1, o2) => {return o1.title.localeCompare(o2.title);}).map(this.formatOutput)}
                  </span>
                  }
                </Col>
              </div>

              <div className="form-group">
                <Col sm={2}>
                  <label className="control-label" htmlFor="streams">Streams</label>
                </Col>
                <Col sm={10}>
                  {this.isEmpty(this.state.streams) ?
                  <span className="help-block help-standalone">There are no streams to export.</span>
                    :
                  <span>
                      <Button className="btn btn-sm btn-link select-all" onClick={this.selectAll.bind(this, 'stream')}>Select all</Button>
                    {this.state.streams.sort((s1, s2) => { return s1.title.localeCompare(s2.title); }).map(this.formatStream)}
                  </span>
                    }
                </Col>
              </div>

              <div className="form-group">
                <Col sm={2}>
                  <label className="control-label" htmlFor="dashboards">Dashboards</label>
                </Col>
                <Col sm={10}>
                  {this.isEmpty(this.state.dashboards) ?
                  <span className="help-block help-standalone">There are no dashboards to export.</span>
                    :
                  <span>
                      <Button className="btn btn-sm btn-link select-all" onClick={this.selectAll.bind(this, 'dashboard')}>Select all</Button>
                      {this.state.dashboards.sort((d1, d2) => {return d1.title.localeCompare(d2.title); }).map(this.formatDashboard)}
                    </span>
                    }
                </Col>
              </div>

              <div className="form-group">
                <Col smOffset={2} sm={10}>
                  <Button download="data:application/csv;charset=utf-8,foo,bar,baz" bsStyle="success" type="submit">
                    <i className="fa fa-cloud-download"/> Download my content pack
                  </Button>

                  <br /><br />
                  <p>
                    <i className="fa fa-lightbulb-o"/>&nbsp;
                    Share your content pack with the community on <a href="https://www.graylog.org/resources/data-sources/" target="_blank">graylog.org</a> after you have downloaded it.
                  </p>
                </Col>
              </div>
            </form>
          </Col>
        </Row>
      </span>
    );
  }
});

export default ExportContentPackPage;
