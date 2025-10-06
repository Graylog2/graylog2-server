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
import styled, { css } from 'styled-components';

import SectionGrid from 'components/common/Section/SectionGrid';
import { CurrentCASection } from 'components/cluster-configuration/certificate-management/CurrentCA';
import { CertificateRenewalPolicyConfigSection } from 'components/cluster-configuration/certificate-management/CertificateRenewal';
import { ClientCertificateConfigSection } from 'components/cluster-configuration/certificate-management/ClientCertificate';

const StyledSectionGrid = styled(SectionGrid)(
  ({ theme }) => css`
    grid-template-rows: '1fr 1fr';
    grid-template-columns: '1fr 1fr';
    gap: ${theme.spacings.md};
  `,
);

const CertificateManagement = () => (
  <StyledSectionGrid>
    <CertificateRenewalPolicyConfigSection />
    <ClientCertificateConfigSection />
    <CurrentCASection />
  </StyledSectionGrid>
);

export default CertificateManagement;
