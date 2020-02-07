import React from 'react';
import PropTypes from 'prop-types';
import styled, { css } from 'styled-components';
// eslint-disable-next-line no-restricted-imports
import { Panel as BootstrapPanel } from 'react-bootstrap';
import { adjustHue, darken } from 'polished';

import { teinte, util } from 'theme';
import bsStyleThemeVariant from './variants/bsStyle';

const backgroundColor = hex => util.colorLevel(hex, -9);
const borderColor = hex => darken(0.05, adjustHue(-10, hex));

const PanelHeading = styled(BootstrapPanel.Heading)``;

const PanelFooter = styled(BootstrapPanel.Footer)`
  background-color: ${teinte.secondary.tre};
  border-top-color: ${teinte.secondary.due};
`;

const panelVariantStyles = hex => css`
  border-color: ${borderColor(hex)};

  & > ${PanelHeading} {
    color: ${colorLevel(backgroundColor(hex), 9)};
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

const StyledPanel = styled(BootstrapPanel)`
  background-color: ${teinte.primary.due};

  .panel-group {
    ${PanelHeading} {
      + .panel-collapse > .panel-body,
      + .panel-collapse > .list-group {
        border-top-color: ${teinte.secondary.due};
      }
    }

    ${PanelFooter} {
      + .panel-collapse .panel-body {
        border-bottom-color: ${teinte.secondary.due};
      }
    }
  }

  ${bsStyleThemeVariant(panelVariantStyles)};
`;

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
const DeprecatedStyledPanel = styled(BootstrapPanel)`
  background-color: ${teinte.primary.due};

  .panel-footer {
    background-color: ${teinte.secondary.tre};
    border-top-color: ${teinte.secondary.due};
  }
  .panel-group {
    .panel-heading {
      + .panel-collapse > .panel-body,
      + .panel-collapse > .list-group {
        border-top-color: ${teinte.secondary.due};
      }
    }
    .panel-footer {
      + .panel-collapse .panel-body {
        border-bottom-color: ${teinte.secondary.due};
      }
    }
  }

  ${bsStyleThemeVariant(deprecatedVariantStyles)}
`;

const CollapsibleBody = ({ children }) => {
  return (
    <DeprecatedStyledPanel.Collapse>
      <DeprecatedStyledPanel.Body>
        {children}
      </DeprecatedStyledPanel.Body>
    </DeprecatedStyledPanel.Collapse>
  );
};

const Panel = ({ header, footer, children, collapsible, expanded, title, ...props }) => {
  /** NOTE: Deprecated & should be removed in 4.0 */
  if (header || footer || title || collapsible || typeof children === 'string') {
    /* eslint-disable-next-line no-console */
    console.warn('Panel: ', 'You have used a deprecated `Panel` prop, please check the documentation to use the latest props.');

    return (
      /* NOTE: this exists as a deprecated render for older Panel instances */
      <DeprecatedStyledPanel {...props} expanded={expanded}>
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

  return <StyledPanel expanded={expanded} {...props}>{children}</StyledPanel>;
};

CollapsibleBody.propTypes = {
  children: PropTypes.any.isRequired,
};

Panel.propTypes = {
  children: PropTypes.any.isRequired,
  /** @deprecated No longer used, replace with `<Panel.Collapse />` &  `expanded`. */
  collapsible: PropTypes.bool,
  /** Must be used in conjunction with `<Panel.Collapse />` */
  expanded: PropTypes.bool,
  /** @deprecated No longer used, replace with `<Panel.Footer />`. */
  footer: PropTypes.string,
  /** @deprecated No longer used, replace with `<Panel.Heading />`. */
  header: PropTypes.string,
  /** @deprecated No longer used, replace with `<Panel.Title />`. */
  title: PropTypes.string,
};

Panel.defaultProps = {
  collapsible: false,
  expanded: null,
  footer: undefined,
  header: undefined,
  title: undefined,
};

Panel.Body = BootstrapPanel.Body;
Panel.Collapse = BootstrapPanel.Collapse;
Panel.Footer = PanelFooter;
Panel.Heading = PanelHeading;
Panel.Title = BootstrapPanel.Title;
Panel.Toggle = BootstrapPanel.Toggle;

export default Panel;
