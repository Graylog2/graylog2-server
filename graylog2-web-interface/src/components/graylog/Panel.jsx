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
import React, { useEffect, useState } from 'react';
import PropTypes from 'prop-types';
import styled, { css } from 'styled-components';
// eslint-disable-next-line no-restricted-imports
import { Panel as BootstrapPanel } from 'react-bootstrap';

import deprecationNotice from 'util/deprecationNotice';

const PanelHeading = styled(BootstrapPanel.Heading)``;

const PanelFooter = styled(BootstrapPanel.Footer)(({ theme }) => css`
  background-color: ${theme.colors.gray[90]};
  border-top-color: ${theme.colors.gray[80]};
`);

const panelVariantStyles = css(({ bsStyle = 'default', theme }) => {
  const backgroundColor = theme.colors.variant.lighter[bsStyle];
  const borderColor = theme.colors.variant.dark[bsStyle];

  return css`
    border-color: ${borderColor};

    > ${PanelHeading} {
      color: ${theme.utils.readableColor(backgroundColor)};
      background-color: ${backgroundColor};
      border-color: ${borderColor};

      + .panel-collapse > .panel-body {
        border-top-color: ${borderColor};
      }

      .badge {
        color: ${backgroundColor};
        background-color: ${theme.colors.variant[bsStyle]};
      }
    }

    > ${PanelFooter} {
      + .panel-collapse > .panel-body {
        border-bottom-color: ${borderColor};
      }
    }
  `;
});

const StyledPanel = styled(BootstrapPanel)(({ theme }) => css`
  background-color: ${theme.utils.colorLevel(theme.colors.global.background, -4)};

  > ${PanelHeading} {
    .panel-title,
    .panel-title h3 {
      font-size: ${theme.fonts.size.large};
    }
  }

  .panel-group {
    > ${PanelHeading} {
      + .panel-collapse > .panel-body,
      + .panel-collapse > .list-group {
        border-top-color: ${theme.colors.gray[90]};
      }
    }

    > ${PanelFooter} {
      + .panel-collapse .panel-body {
        border-bottom-color: ${theme.colors.gray[90]};
      }
    }
  }

  ${panelVariantStyles}
`);

const deprecatedVariantStyles = css(({ bsStyle = 'default', theme }) => {
  const backgroundColor = theme.colors.variant.lightest[bsStyle];
  const borderColor = theme.colors.variant.light[bsStyle];

  return css`
    /** NOTE: Deprecated & should be removed in 4.0 */
    border-color: ${borderColor};
    background: ${theme.colors.table.background};

    & > .panel-heading {
      color: ${theme.utils.contrastingColor(backgroundColor)};
      background-color: ${backgroundColor};
      border-color: ${borderColor};

      > .panel-title,
      > .panel-title > * {
        font-size: ${theme.fonts.size.large};
      }

      + .panel-collapse > .panel-body {
        border-top-color: ${borderColor};
      }

      .badge {
        color: ${backgroundColor};
        background-color: ${theme.colors.variant[bsStyle]};
      }
    }

    & > .panel-footer {
      + .panel-collapse > .panel-body {
        border-bottom-color: ${borderColor};
      }
    }
  `;
});

const DeprecatedStyledPanel = styled(BootstrapPanel)(({ theme }) => css`
  /** NOTE: Deprecated & should be removed in 4.0 */
  background-color: ${theme.utils.colorLevel(theme.colors.global.background, -4)};

  .panel-footer {
    background-color: ${theme.colors.gray[90]};
    border-top-color: ${theme.colors.gray[80]};
  }

  .panel-group {
    .panel-heading {
      + .panel-collapse > .panel-body,
      + .panel-collapse > .list-group {
        border-top-color: ${theme.colors.gray[90]};
      }
    }

    .panel-footer {
      + .panel-collapse .panel-body {
        border-bottom-color: ${theme.colors.gray[90]};
      }
    }
  }

  ${deprecatedVariantStyles}
`);

const Panel = ({
  title,
  children,
  collapsible,
  defaultExpanded,
  expanded,
  footer,
  header,
  onToggle,
  ...props
}) => {
  const [isExpanded, setIsExpanded] = useState(false);

  useEffect(() => {
    setIsExpanded((defaultExpanded && expanded)
      || (!defaultExpanded && expanded)
      || (defaultExpanded && isExpanded === expanded));
  }, [expanded]);

  const handleToggle = (nextIsExpanded) => {
    setIsExpanded(nextIsExpanded);
    onToggle(nextIsExpanded);
  };

  const hasDeprecatedChildren = typeof children === 'string' || (Array.isArray(children) && typeof children[0] === 'string');

  if (header || footer || title || collapsible || hasDeprecatedChildren) {
    /** NOTE: Deprecated & should be removed in 4.0 */
    useEffect(() => {
      deprecationNotice('You have used a deprecated `Panel` prop, please check the documentation to use the latest `Panel`.');
    }, []);

    return (
      /* NOTE: this exists as a deprecated render for older Panel instances */
      <DeprecatedStyledPanel expanded={isExpanded}
                             onToggle={handleToggle}
                             {...props}>
        {(header || title) && (
          <PanelHeading>
            {header}
            {title && <BootstrapPanel.Title toggle={collapsible}>{title}</BootstrapPanel.Title>}
          </PanelHeading>
        )}
        <DeprecatedStyledPanel.Body collapsible={collapsible}>
          {children}
        </DeprecatedStyledPanel.Body>
        {footer && (
          <DeprecatedStyledPanel.Footer>{footer}</DeprecatedStyledPanel.Footer>
        )}
      </DeprecatedStyledPanel>
    );
  }

  return (
    <StyledPanel expanded={isExpanded}
                 onToggle={handleToggle}
                 defaultExpanded={defaultExpanded}
                 {...props}>
      {children}
    </StyledPanel>
  );
};

Panel.propTypes = {
  children: PropTypes.any.isRequired,
  /** @deprecated No longer used, replace with `<Panel.Collapse />` &  `expanded`. */
  collapsible: PropTypes.bool,
  /**
   * Default of `expanded` prop
   *
   * @controllable onToggle
   */
  defaultExpanded: PropTypes.bool,
  /**
   * Controls the collapsed/expanded state ofthe Panel. Requires
   * a `Panel.Collapse` or `<Panel.Body collapsible>` child component
   * in order to actually animate out or in.
   *
   * @controllable onToggle
   */
  expanded: PropTypes.bool,
  /**
   * A callback fired when the collapse state changes.
   *
   * @controllable expanded
   */
  onToggle: PropTypes.func,
  /** @deprecated No longer used, replace with `<Panel.Footer />`. */
  footer: PropTypes.string,
  /** @deprecated No longer used, replace with `<Panel.Heading />`. */
  header: PropTypes.oneOfType([PropTypes.string, PropTypes.node]),
  /** @deprecated No longer used, replace with `<Panel.Title />`. */
  title: PropTypes.string,
};

Panel.defaultProps = {
  collapsible: false,
  defaultExpanded: null,
  expanded: false,
  footer: undefined,
  header: undefined,
  onToggle: () => {},
  title: undefined,
};

Panel.Body = BootstrapPanel.Body;
Panel.Collapse = BootstrapPanel.Collapse;
Panel.Footer = PanelFooter;
Panel.Heading = PanelHeading;
Panel.Title = BootstrapPanel.Title;
Panel.Toggle = BootstrapPanel.Toggle;

/** @component */
export default Panel;
