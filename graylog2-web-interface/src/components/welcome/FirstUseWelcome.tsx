import * as React from 'react';
import styled from 'styled-components';

import useProductName from 'brand-customization/useProductName';
import SectionComponent from 'components/common/Section/SectionComponent';
import LinkContainer from 'components/common/LinkContainer';

import PageHeader from '../common/PageHeader';
const StyledSectionComponent = styled(SectionComponent)`
  flex-grow: 1;
`;
const Description = ({ children = undefined }: React.PropsWithChildren<{}>) => (
  <p className="description">{children}</p>
);

const resources = [
  {
    title: 'Quickstart Guide',
    description: 'End-to-end walkthrough to your first search in 10 minutes.',
    link: 'https://www.graylog.org',
  },
  {
    title: 'Video: Sidecar',
    description: 'Setup Install and configure your first collector agent.',
    link: 'https://www.graylog.org',
  },
  {
    title: 'Community Forum',
    description: 'Ask questions and browse solutions from other Graylog users.',
    link: 'https://www.graylog.org',
  },
];

const Section = styled.div`
  display: flex;
  column-gap: 40px;
`;

const Resources = styled.h3`
  padding-top: 2rem;
  padding-bottom: 1rem;
`;

const ActionsSection = styled(Section)`
  padding-top: 1rem;
`;

const FirstUseWelcome = () => {
  const productName = useProductName();

  return (
    <>
      <PageHeader title={`Welcome to ${productName}!`}>
        <span>
          Graylog connects to dozens of sources; servers, firewalls, cloud apps, and more. Where would you like to
          start?
        </span>
      </PageHeader>
      <ActionsSection>
        <StyledSectionComponent title="Endpoint Logging">
          <Description>
            Install a lightweight agent on your servers, VMs, or containers. Graylog Sidecar manages the configuration
            automatically.
          </Description>
        </StyledSectionComponent>
        <StyledSectionComponent title="Other Data Sources">
          <Description>
            Open a network listener that accepts logs directly over GELF, Syslog, Beats, or other protocols.
          </Description>
        </StyledSectionComponent>
      </ActionsSection>

      <Resources>Resources</Resources>

      <Section>
        {resources.map((resource) => (
          <LinkContainer key={resource.title} to={resource.link}>
            <StyledSectionComponent title={resource.title}>
              <Description>{resource.description}</Description>
            </StyledSectionComponent>
          </LinkContainer>
        ))}
      </Section>
    </>
  );
};
export default FirstUseWelcome;
