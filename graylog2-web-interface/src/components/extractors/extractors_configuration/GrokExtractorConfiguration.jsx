import PropTypes from 'prop-types';
import React from 'react';
import { Row, Col, Button } from 'react-bootstrap';

import { Input } from 'components/bootstrap';
import GrokPatternInput from 'components/grok-patterns/GrokPatternInput';
import UserNotification from 'util/UserNotification';
import FormUtils from 'util/FormsUtils';
import StoreProvider from 'injection/StoreProvider';

import Style from './GrokExtractorConfiguration.css';

const ToolsStore = StoreProvider.getStore('Tools');
const GrokPatternsStore = StoreProvider.getStore('GrokPatterns');

class GrokExtractorConfiguration extends React.Component {
  static propTypes = {
    configuration: PropTypes.object.isRequired,
    exampleMessage: PropTypes.string,
    onChange: PropTypes.func.isRequired,
    onExtractorPreviewLoad: PropTypes.func.isRequired,
  };

  static defaultProps = {
    exampleMessage: undefined,
  };

  state = {
    trying: false,
    patterns: [],
  };

  componentDidMount() {
    this.loadData();
  }

  componentWillUnmount() {
    if (this.loadPromise) {
      this.loadPromise.cancel();
    }
  }

  loadData = () => {
    this.loadPromise = GrokPatternsStore.loadPatterns((patterns) => {
      if (!this.loadPromise.isCancelled()) {
        this.loadPromise = undefined;
        this.setState({
          patterns: patterns,
        });
      }
    });
  };

  _onChange = (key) => {
    return (event) => {
      this.props.onExtractorPreviewLoad(undefined);
      const newConfig = this.props.configuration;
      newConfig[key] = FormUtils.getValueFromInput(event.target);
      this.props.onChange(newConfig);
    };
  };

  _onPatternChange = (newPattern) => {
    this.props.onExtractorPreviewLoad(undefined);
    const newConfig = this.props.configuration;
    newConfig.grok_pattern = newPattern;
    this.props.onChange(newConfig);
  };

  _onTryClick = () => {
    this.setState({ trying: true });

    const promise = ToolsStore.testGrok(this.props.configuration.grok_pattern, this.props.configuration.named_captures_only, this.props.exampleMessage);
    promise.then((result) => {
      if (result.error_message != null) {
        UserNotification.error(`We were not able to run the grok extraction because of the following error: ${result.error_message}`);
        return;
      }

      if (!result.matched) {
        UserNotification.warning('We were not able to run the grok extraction. Please check your parameters.');
        return;
      }

      const matches = [];
      result.matches.forEach((match) => {
        matches.push(<dt key={`${match.name}-name`}>{match.name}</dt>);
        matches.push(<dd key={`${match.name}-value`}><samp>{match.match}</samp></dd>);
      });

      const preview = (matches.length === 0 ? '' : <dl>{matches}</dl>);
      this.props.onExtractorPreviewLoad(preview);
    });

    promise.finally(() => this.setState({ trying: false }));
  };

  _isTryButtonDisabled = () => {
    return this.state.trying || !this.props.configuration.grok_pattern || !this.props.exampleMessage;
  };

  render() {
    return (
      <div>
        <Input type="checkbox"
               id="named_captures_only"
               label="Named captures only"
               wrapperClassName="col-md-offset-2 col-md-10"
               defaultChecked={this.props.configuration.named_captures_only}
               onChange={this._onChange('named_captures_only')}
               help="Only put the explicitly named captures into the message." />

        <Input id="grok_pattern"
               label="Grok pattern"
               labelClassName="col-md-2"
               wrapperClassName="col-md-10">
          <Row className="row-sm">
            <Col md={11}>
              <GrokPatternInput
                onPatternChange={this._onPatternChange}
                pattern={this.props.configuration.grok_pattern || ''}
                patterns={this.state.patterns}
                className={Style.grokInput}
              />
            </Col>
          </Row>
          <Row>
            <Col md={1}>
              <Button bsStyle="info" onClick={this._onTryClick} disabled={this._isTryButtonDisabled()}>
                {this.state.trying ? <i className="fa fa-spin fa-spinner" /> : 'Try against example'}
              </Button>
            </Col>
          </Row>
        </Input>
      </div>
    );
  }
}

export default GrokExtractorConfiguration;
