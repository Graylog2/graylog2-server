/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import React from 'react';
import PropTypes from 'prop-types';

import { Row, Col, ControlLabel, Button } from 'components/graylog';
import { Icon } from 'components/common';
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
    const { onChange, onExtractorPreviewLoad, configuration } = this.props;

    return (event) => {
      onExtractorPreviewLoad(undefined);
      const newConfig = configuration;

      newConfig[key] = FormUtils.getValueFromInput(event.target);
      onChange(newConfig);
    };
  };

  _onPatternChange = (newPattern) => {
    const { onChange, onExtractorPreviewLoad, configuration } = this.props;

    onExtractorPreviewLoad(undefined);
    const newConfig = configuration;

    newConfig.grok_pattern = newPattern;
    onChange(newConfig);
  };

  _onTryClick = () => {
    const { exampleMessage, configuration, onExtractorPreviewLoad } = this.props;

    this.setState({ trying: true });

    const promise = ToolsStore.testGrok(configuration.grok_pattern, configuration.named_captures_only, exampleMessage);

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

      onExtractorPreviewLoad(preview);
    });

    promise.finally(() => this.setState({ trying: false }));
  };

  _isTryButtonDisabled = () => {
    const { trying } = this.state;
    const { configuration, exampleMessage } = this.props;

    return trying || !configuration.grok_pattern || !exampleMessage;
  };

  render() {
    const { patterns, trying } = this.state;
    const { configuration } = this.props;

    return (
      <div>
        <Input type="checkbox"
               id="named_captures_only"
               label="Named captures only"
               wrapperClassName="col-md-offset-2 col-md-10"
               defaultChecked={configuration.named_captures_only}
               onChange={this._onChange('named_captures_only')}
               help="Only put the explicitly named captures into the message." />

        <Row>
          <Col mdOffset={1} md={1}>
            <ControlLabel className="col-md-offset-2">Grok pattern</ControlLabel>
          </Col>
          <Col md={10}>
            <GrokPatternInput onPatternChange={this._onPatternChange}
                              pattern={configuration.grok_pattern || ''}
                              patterns={patterns}
                              className={Style.grokInput} />
          </Col>
        </Row>
        <Row>
          <Col mdOffset={2} md={1}>
            <Button bsStyle="info" onClick={this._onTryClick} disabled={this._isTryButtonDisabled()}>
              {trying ? <Icon name="spinner" spin /> : 'Try against example'}
            </Button>
          </Col>
        </Row>
      </div>
    );
  }
}

export default GrokExtractorConfiguration;
