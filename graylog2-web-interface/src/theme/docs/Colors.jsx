import React from 'react';
import styled, { withTheme } from 'styled-components';
import chroma from 'chroma-js';

import ColorSwatch, { Swatch } from './ColorSwatch';

const Section = styled.h4`
  margin: 0 0 6px;
`;

const Swatches = styled.div`
  display: flex;
  margin: 0 0 15px;
  flex-wrap: wrap;
`;

const StyledColorSwatch = styled(ColorSwatch)`
  flex-basis: ${(props) => (props.section === 'global' ? '100px' : '1px')};
  max-width: ${(props) => (props.section === 'global' ? '200px' : 'auto')};

  ${Swatch} {
    margin-right: 6px;
    margin-bottom: 3px;

    &:last-of-type {
      margin: 0;
    }
  }
`;

const getValues = (data = {}, callback = () => {}) => {
  return Object.keys(data).map((key) => callback(key));
};

const SectionWrap = (mode, section) => {
  return (
    <React.Fragment key={`swatches-${section}`}>
      <Swatches>
        {getValues(mode, (name) => {
          if (typeof mode[name] === 'string' && chroma.valid(mode[name])) {
            const copyTextName = section === 'gray' ? `${section}[${name}]` : `${section}.${name}`;

            return (
              <StyledColorSwatch name={name}
                                 section={section}
                                 color={mode[name]}
                                 copyText={`theme.colors.${copyTextName}`}
                                 key={`color-swatch-${section}-${copyTextName}`} />
            );
          }
        })}
      </Swatches>

      <div>
        {getValues(mode, (name) => typeof mode[name] === 'object' && (
        <React.Fragment key={`swatches-${section}-${name}`}>
          <Section>{section} &mdash; {name}</Section>

          <Swatches>
            {getValues(mode[name], (subname) => (
              <StyledColorSwatch name={subname}
                                 section={section}
                                 color={mode[name][subname]}
                                 copyText={`theme.colors.${section}.${name}.${subname}`}
                                 key={`color-swatch-${section}-${name}-${subname}`} />
            ))}
          </Swatches>
        </React.Fragment>
        ))}
      </div>
    </React.Fragment>
  );
};

const Colors = ({ theme }) => {
  return (
    <>
      {getValues(theme.colors, (section) => (
        <React.Fragment key={`section-${section}`}>
          <Section>{section}</Section>
          {SectionWrap(theme.colors[section], section)}
        </React.Fragment>
      ))}
    </>
  );
};

export default withTheme(Colors);
