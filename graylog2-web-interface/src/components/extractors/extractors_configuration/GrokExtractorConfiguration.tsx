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
import { withTranslation } from 'react-i18next';
import type { WithTranslation } from 'react-i18next';

import { Icon } from 'components/common';
import { Row, Col, ControlLabel, Button, Input } from 'components/bootstrap';
import GrokPatternInput from 'components/grok-patterns/GrokPatternInput';
import UserNotification from 'util/UserNotification';
import { getValueFromInput } from 'util/FormsUtils';
import { testGrok } from 'api/tools';
import { loadGrokPatterns } from 'hooks/useGrokPatterns';

import Style from './GrokExtractorConfiguration.css';

type Props = {
  configuration: any;
  exampleMessage?: string;
  onChange: (...args: any[]) => void;
  onExtractorPreviewLoad: (...args: any[]) => void;
} & WithTranslation;

class GrokExtractorConfiguration extends React.Component<
  Props,
  {
    trying: boolean;
    patterns: Array<any>;
  }
> {
  static defaultProps = {
    exampleMessage: undefined,
  };

  constructor(props: Props) {
    super(props);
    this.state = {
      trying: false,
      patterns: [],
    };
  }

  componentDidMount() {
    this.loadData();
  }

  componentWillUnmount() {
    this._unmounted = true;
  }

  private _unmounted = false;

  loadData = () => {
    loadGrokPatterns((patterns) => {
      if (!this._unmounted) {
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

      newConfig[key] = getValueFromInput(event.target);
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

    const promise = testGrok(configuration.grok_pattern, configuration.named_captures_only, exampleMessage);

    promise.then((result) => {
      if (result.error_message != null) {
        UserNotification.error(this.props.t('grok.error', { error: result.error_message }));

        return;
      }

      if (!result.matched) {
        UserNotification.warning(this.props.t('grok.no_match'));

        return;
      }

      const matches = [];

      result.matches.forEach((match) => {
        matches.push(<dt key={`${match.name}-name`}>{match.name}</dt>);
        matches.push(
          <dd key={`${match.name}-value`}>
            <samp>{match.match}</samp>
          </dd>,
        );
      });

      const preview = matches.length === 0 ? '' : <dl>{matches}</dl>;

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
    const { configuration, t } = this.props;

    return (
      <div>
        <Input
          type="checkbox"
          id="named_captures_only"
          label={t('grok.named_captures_only')}
          wrapperClassName="col-md-offset-2 col-md-10"
          defaultChecked={configuration.named_captures_only}
          onChange={this._onChange('named_captures_only')}
          help={t('grok.named_captures_only_help')}
        />

        <Row>
          <Col mdOffset={1} md={1}>
            <ControlLabel className="col-md-offset-2">{t('grok.pattern')}</ControlLabel>
          </Col>
          <Col md={10}>
            <GrokPatternInput
              onPatternChange={this._onPatternChange}
              pattern={configuration.grok_pattern || ''}
              patterns={patterns}
              className={Style.grokInput}
            />
          </Col>
        </Row>
        <Row>
          <Col mdOffset={2} md={1}>
            <Button bsStyle="info" onClick={this._onTryClick} disabled={this._isTryButtonDisabled()}>
              {trying ? <Icon name="progress_activity" spin /> : t('configuration.try_example')}
            </Button>
          </Col>
        </Row>
      </div>
    );
  }
}

export default withTranslation('extractors')(GrokExtractorConfiguration);
