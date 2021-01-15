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
import { Portal } from 'react-portal';
import { Position } from 'react-overlays';
import styled, { css } from 'styled-components';

import { Popover } from 'components/graylog';
import { Icon, HoverForHelp } from 'components/common';

const StyledDescriptionBox = styled.div(({ theme }) => css`
  background-color: ${theme.colors.variant.lightest.default};
  border: 1px solid ${theme.colors.variant.lighter.default};
  padding: 10px;
  margin: 5px;
  border-radius: 6px;

  .description {
    padding-bottom: 5px;
    text-transform: uppercase;
  }
`);

const ConfigButton = styled.button`
  border: 0;
  background: transparent;
  padding: 0;
`;

class DescriptionBox extends React.Component {
  constructor(props) {
    super(props);

    this.state = {
      configOpen: false,
    };
  }

  onToggleConfig = () => {
    const { configOpen } = this.state;

    this.setState({ configOpen: !configOpen });
  };

  configPopover = () => {
    const { configOpen } = this.state;
    const { configurableOptions } = this.props;

    if (!configOpen) {
      return '';
    }

    const configurableElement = React.cloneElement(configurableOptions, {
      onClose: this.onToggleConfig,
    });

    return (
      <Portal>
        <Position container={document.body}
                  placement="bottom"
                  target={this.target}>
          <Popover title="Config options" id="config-popover">
            {configurableElement}
          </Popover>
        </Position>
      </Portal>
    );
  };

  configCaret = () => {
    const { configurableOptions } = this.props;

    if (configurableOptions) {
      return (
        <ConfigButton ref={(node) => { this.target = node; }}
                      type="button"
                      onClick={this.onToggleConfig}>
          <Icon name="wrench" />
        </ConfigButton>
      );
    }

    return null;
  };

  render() {
    const { description, children, help, style: inlineStyle } = this.props;

    return (
      <StyledDescriptionBox style={inlineStyle}>
        <div className="description">
          {description}
          {this.configCaret()}
          {help && <HoverForHelp title={description}>{help}</HoverForHelp>}
        </div>
        {children}
        {this.configPopover()}
      </StyledDescriptionBox>
    );
  }
}

DescriptionBox.propTypes = {
  children: PropTypes.node.isRequired,
  configurableOptions: PropTypes.node,
  description: PropTypes.string.isRequired,
  help: PropTypes.string,
  style: PropTypes.object,
};

DescriptionBox.defaultProps = {
  configurableOptions: undefined,
  style: undefined,
  help: undefined,
};

export default DescriptionBox;
