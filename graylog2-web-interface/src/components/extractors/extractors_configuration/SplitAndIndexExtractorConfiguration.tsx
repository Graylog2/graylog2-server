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
import PropTypes from 'prop-types';
import * as React from 'react';
import { useCallback, useEffect, useState } from 'react';

import { Icon } from 'components/common';
import { Col, Row, Button, Input } from 'components/bootstrap';
import UserNotification from 'util/UserNotification';
import ExtractorUtils from 'util/ExtractorUtils';
import FormUtils from 'util/FormsUtils';
import ToolsStore from 'stores/tools/ToolsStore';

const DEFAULT_CONFIGURATION = { index: 1 };

const _getEffectiveConfiguration = (configuration) => ExtractorUtils.getEffectiveConfiguration(DEFAULT_CONFIGURATION, configuration);

type Configuration = { [key: string]: string };
type Props = {
  configuration: Configuration,
  exampleMessage: string,
  onChange: (newConfig: Configuration) => void,
  onExtractorPreviewLoad: (preview: React.ReactNode | string) => void,
}

const SplitAndIndexExtractorConfiguration = ({ configuration: initialConfiguration, exampleMessage, onChange, onExtractorPreviewLoad }: Props) => {
  const [configuration, setConfiguration] = useState(_getEffectiveConfiguration(initialConfiguration));
  useEffect(() => { setConfiguration(_getEffectiveConfiguration(initialConfiguration)); }, [initialConfiguration]);

  const [trying, setTrying] = useState(false);

  const _onChange = (key: string) => {
    return (event) => {
      onExtractorPreviewLoad(undefined);
      const newConfig = configuration;

      newConfig[key] = FormUtils.getValueFromInput(event.target);
      onChange(newConfig);
    };
  };

  const _onTryClick = useCallback(() => {
    setTrying(true);

    const promise = ToolsStore.testSplitAndIndex(configuration.split_by, configuration.index, exampleMessage);

    promise.then((result) => {
      if (!result.successful) {
        UserNotification.warning('We were not able to run the split and index extraction. Please check your parameters.');

        return;
      }

      const preview = (result.cut ? <samp>{result.cut}</samp> : '');

      onExtractorPreviewLoad(preview);
    });

    promise.finally(() => setTrying(false));
  }, [configuration.index, configuration.split_by, exampleMessage, onExtractorPreviewLoad]);

  const splitByHelpMessage = (
    <span>
      What character to split on. <strong>Example:</strong> A whitespace character will split{' '}
      <em>foo bar baz</em> to <em>[foo,bar,baz]</em>.
    </span>
  );

  const indexHelpMessage = (
    <span>
      What part of the split string to you want to use? <strong>Example:</strong> <em>2</em> selects <em>bar</em>{' '}
      from <em>foo bar baz</em> when split by whitespace.
    </span>
  );

  const isTryButtonDisabled = trying || configuration.split_by === '' || configuration.index === undefined || configuration.index < 1 || !exampleMessage;

  return (
    <div>
      <Input type="text"
             id="split_by"
             label="Split by"
             labelClassName="col-md-2"
             wrapperClassName="col-md-10"
             defaultValue={configuration.split_by}
             onChange={_onChange('split_by')}
             required
             help={splitByHelpMessage} />

      <Input type="number"
             id="index"
             label="Target index"
             labelClassName="col-md-2"
             wrapperClassName="col-md-10"
             defaultValue={configuration.index}
             onChange={_onChange('index')}
             min="1"
             required
             help={indexHelpMessage} />

      <Row>
        <Col mdOffset={2} md={10}>
          <Button bsStyle="info" onClick={_onTryClick} disabled={isTryButtonDisabled}>
            {trying ? <Icon name="spinner" spin /> : 'Try'}
          </Button>
        </Col>
      </Row>
    </div>
  );
};

SplitAndIndexExtractorConfiguration.propTypes = {
  configuration: PropTypes.object.isRequired,
  exampleMessage: PropTypes.string,
  onChange: PropTypes.func.isRequired,
  onExtractorPreviewLoad: PropTypes.func.isRequired,
};

SplitAndIndexExtractorConfiguration.defaultProps = {
  exampleMessage: undefined,
};

export default SplitAndIndexExtractorConfiguration;
