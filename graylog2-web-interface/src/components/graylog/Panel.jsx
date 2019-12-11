import React from 'react';
import PropTypes from 'prop-types';
import styled, { css } from 'styled-components';
// eslint-disable-next-line no-restricted-imports
import { Panel as BootstrapPanel } from 'react-bootstrap';
import { adjustHue, darken } from 'polished';

import { util } from 'theme';
import bsStyleThemeVariant from './variants/bsStyle';

const backgroundColor = hex => util.colorLevel(hex, -9);
const borderColor = hex => darken(0.05, adjustHue(-10, hex));

const PanelHeading = styled(BootstrapPanel.Heading)``;

const PanelFooter = styled(BootstrapPanel.Footer)(({ theme }) => css`
  background-color: ${theme.color.secondary.tre};
  border-top-color: ${theme.color.secondary.due};
`);

const panelVariantStyles = hex => css`
  border-color: ${borderColor(hex)};

  & > ${PanelHeading} {
    color: ${util.colorLevel(backgroundColor(hex), 9)};
    background-color: ${backgroundColor(hex)};
    border-color: ${borderColor(hex)};

    + .panel-collapse > .panel-body {
      border-top-color: ${borderColor(hex)};
    }

    .badge {
      color: ${backgroundColor(hex)};
      background-color: ${hex};
    }
  }

  & > ${PanelFooter} {
    + .panel-collapse > .panel-body {
      border-bottom-color: ${borderColor(hex)};
    }
  }
`;

const StyledPanel = styled(BootstrapPanel)(({ theme }) => css`
  background-color: ${theme.color.primary.due};

  .panel-group {
    ${PanelHeading} {
      + .panel-collapse > .panel-body,
      + .panel-collapse > .list-group {
        border-top-color: ${theme.color.secondary.due};
      }
    }

    ${PanelFooter} {
      + .panel-collapse .panel-body {
        border-bottom-color: ${theme.color.secondary.due};
      }
    }
  }

  ${bsStyleThemeVariant(panelVariantStyles)};
`);

/** NOTE: Deprecated & should be removed in 4.0 */
const deprecatedVariantStyles = hex => css`
  border-color: ${borderColor(hex)};

  & > .panel-heading {
    color: ${util.colorLevel(backgroundColor(hex), 9)};
    background-color: ${backgroundColor(hex)};
    border-color: ${borderColor(hex)};

    + .panel-collapse > .panel-body {
      border-top-color: ${borderColor(hex)};
    }

    .badge {
      color: ${backgroundColor(hex)};
      background-color: ${hex};
    }
  }

  & > .panel-footer {
    + .panel-collapse > .panel-body {
      border-bottom-color: ${borderColor(hex)};
    }
  }
`;

/** NOTE: Deprecated & should be removed in 4.0 */
const DeprecatedStyledPanel = styled(BootstrapPanel)(({ theme }) => css`
  background-color: ${theme.color.primary.due};

  .panel-footer {
    background-color: ${theme.color.secondary.tre};
    border-top-color: ${theme.color.secondary.due};
  }

  .panel-group {
    .panel-heading {
      + .panel-collapse > .panel-body,
      + .panel-collapse > .list-group {
        border-top-color: ${theme.color.secondary.due};
      }
    }

    .panel-footer {
      + .panel-collapse .panel-body {
        border-bottom-color: ${theme.color.secondary.due};
      }
    }
  }

  ${bsStyleThemeVariant(deprecatedVariantStyles)}
`);

const CollapsibleBody = ({ children }) => {
  return (
    <DeprecatedStyledPanel.Collapse>
      <DeprecatedStyledPanel.Body>
        {children}
      </DeprecatedStyledPanel.Body>
    </DeprecatedStyledPanel.Collapse>
  );
};

const Panel = ({
  title,
  children,
  collapsible,
  expanded,
  footer,
  header,
  onToggle,
  ...props
}) => {
  /** NOTE: Deprecated & should be removed in 4.0 */
  if (header || footer || title || collapsible || typeof children === 'string') {
    /* eslint-disable-next-line no-console */
    console.warn('Panel: ', 'You have used a deprecated `Panel` prop, please check the documentation to use the latest props.');

    return (
      /* NOTE: this exists as a deprecated render for older Panel instances */
      <DeprecatedStyledPanel {...props} expanded={expanded} onToggle={onToggle}>
        {header && (
          <DeprecatedStyledPanel.Heading>{header}</DeprecatedStyledPanel.Heading>
        )}
        {collapsible
          ? <CollapsibleBody>{children}</CollapsibleBody>
          : <DeprecatedStyledPanel.Body>{children}</DeprecatedStyledPanel.Body>}
        {footer && (
          <DeprecatedStyledPanel.Footer>{footer}</DeprecatedStyledPanel.Footer>
        )}
      </DeprecatedStyledPanel>
    );
  }

  return (
    <StyledPanel expanded={expanded}
                 onToggle={onToggle}
                 {...props}>
      {children}
    </StyledPanel>
  );
};

CollapsibleBody.propTypes = {
  children: PropTypes.any.isRequired,
};

Panel.propTypes = {
  children: PropTypes.any.isRequired,
  /** @deprecated No longer used, replace with `<Panel.Collapse />` &  `expanded`. */
  collapsible: PropTypes.bool,
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
  header: PropTypes.string,
  /** @deprecated No longer used, replace with `<Panel.Title />`. */
  title: PropTypes.string,
};

Panel.defaultProps = {
  collapsible: false,
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
