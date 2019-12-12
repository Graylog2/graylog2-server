import React, { useState } from 'react';
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

const deprecatedVariantStyles = hex => css`
  /** NOTE: Deprecated & should be removed in 4.0 */
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

const DeprecatedStyledPanel = styled(BootstrapPanel)`
  /** NOTE: Deprecated & should be removed in 4.0 */
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

  React.useEffect(() => {
    setIsExpanded((defaultExpanded && expanded)
      || (!defaultExpanded && expanded)
      || (defaultExpanded && isExpanded === expanded));
  }, [expanded]);

  const handleToggle = (nextIsExpanded) => {
    setIsExpanded(nextIsExpanded);
    onToggle(nextIsExpanded);
  };

  if (header || footer || title || collapsible || typeof children === 'string') {
    /** NOTE: Deprecated & should be removed in 4.0 */
    /* eslint-disable-next-line no-console */
    console.warn('Graylog Notice: ', 'You have used a deprecated `Panel` prop, please check the documentation to use the latest `Panel`.');

    return (
      /* NOTE: this exists as a deprecated render for older Panel instances */
      <DeprecatedStyledPanel expanded={isExpanded}
                             onToggle={handleToggle}
                             {...props}>
        {(header || title) && (
          <DeprecatedStyledPanel.Heading>
            <Panel.Title toggle={collapsible}>
              {header || title}
            </Panel.Title>
          </DeprecatedStyledPanel.Heading>
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

export default Panel;
