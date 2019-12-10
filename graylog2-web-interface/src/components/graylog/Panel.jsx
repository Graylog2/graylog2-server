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

const panelVariantStyles = hex => css`
  border-color: ${borderColor(hex)};
`;

const headingVariantStyles = hex => css`
  && {
    color: ${util.colorLevel(backgroundColor(hex), 9)};
    background-color: ${backgroundColor(hex)};
    border-color: ${borderColor(hex)};

    + .panel-collapse > .panel-body {
      border-top-color: ${borderColor};
    }

    .badge {
      color: ${backgroundColor};
      background-color: ${hex};
    }
  }
`;

const footerVariantStyles = hex => css`
  && {
    color: ${util.colorLevel(backgroundColor(hex), 9)};
    background-color: ${backgroundColor(hex)};
    border-color: ${borderColor(hex)};

    + .panel-collapse > .panel-body {
      border-bottom-color: ${borderColor};
    }
  }
`;

const PanelHeading = styled(BootstrapPanel.Heading)`
  ${bsStyleThemeVariant(headingVariantStyles)};
`;

const PanelFooter = styled(BootstrapPanel.Footer)(({ theme }) => css`
  background-color: ${theme.color.secondary.tre};
  border-top-color: ${theme.color.secondary.due};
  ${bsStyleThemeVariant(footerVariantStyles)};
`);

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

  ${bsStyleThemeVariant(panelVariantStyles)}
`);

const CollapsibleBody = ({ children }) => {
  return (
    <BootstrapPanel.Collapse>
      <BootstrapPanel.Body>
        {children}
      </BootstrapPanel.Body>
    </BootstrapPanel.Collapse>
  );
};

const Panel = ({ header, footer, children, collapsible, expanded, title, ...props }) => {
  if (header || footer || title || collapsible) {
    /* eslint-disable-next-line no-console */
    console.warn('Panel: ', 'You have used a deprecated `Panel` prop, please check the documentation to use the latest props.');

    return (
      /* NOTE: this exists as a deprecated render for older Panel instances */
      <BootstrapPanel {...props} expanded={expanded}>
        {header && (
          <BootstrapPanel.Heading>{header}</BootstrapPanel.Heading>
        )}
        {collapsible
          ? <CollapsibleBody>{children}</CollapsibleBody>
          : <BootstrapPanel.Body>{children}</BootstrapPanel.Body>}
        {footer && (
          <BootstrapPanel.Footer>{footer}</BootstrapPanel.Footer>
        )}
      </BootstrapPanel>
    );
  }

  return <StyledPanel {...props} expanded={expanded}>{children}</StyledPanel>;
};

CollapsibleBody.propTypes = {
  children: PropTypes.any.isRequired,
};

Panel.propTypes = {
  children: PropTypes.any.isRequired,
  /** @deprecated No longer used, replace with `<Panel.Collapse />` &  `expandable`. */
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
  expanded: false,
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

/** @component */
export default Panel;
