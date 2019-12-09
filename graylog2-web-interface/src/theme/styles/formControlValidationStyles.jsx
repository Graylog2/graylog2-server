import { darken, lighten } from 'polished';

import InputGroup from 'components/graylog/InputGroup';
import teinte from '../teinte';
import colorLevel from '../util/colorLevel';

const VARIANTS = [
  {
    success: {
      text: colorLevel(teinte.tertiary.tre, 6),
      border: teinte.tertiary.tre,
      background: colorLevel(teinte.tertiary.tre, -6),
    },
  },
  {
    warning: {
      text: colorLevel(teinte.tertiary.sei, 6),
      border: teinte.tertiary.sei,
      background: colorLevel(teinte.tertiary.sei, -6),
    },
  },
  {
    error: {
      text: colorLevel(teinte.secondary.uno, 6),
      border: teinte.secondary.uno,
      background: colorLevel(teinte.secondary.uno, -6),
    },
  },
];

const generateStyles = () => {
  let styles = '';

  VARIANTS.forEach((variant) => {
    const key = Object.keys(variant)[0];

    styles += `
      .has-${key} {
        .help-block,
        .control-label,
        .radio,
        .checkbox,
        .radio-inline,
        .checkbox-inline,
        &.radio label,
        &.checkbox label,
        &.radio-inline label,
        &.checkbox-inline label  {
          color: ${variant[key].text};
        }

        .form-control {
          border-color: ${variant[key].border};

          &:focus {
            border-color: ${darken(0.10, variant[key].border)};
            box-shadow(inset 0 1px 1px rgba(0, 0, 0, .075), 0 0 6px ${lighten(0.20, variant[key].border)});
          }
        }

        ${InputGroup} .input-group-addon {
          color: ${variant[key].text};
          background-color: ${variant[key].background};
          border-color: ${variant[key].border};
        }

        .form-control-feedback {
          color: ${variant[key].text};
        }
      }
    `;
  });

  return styles;
};

const formControlValidationStyles = generateStyles();

export default formControlValidationStyles;
