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
package org.graylog.security.authservice.ldap;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ADUserAccountControlTest {
    @Test
    void isSetIn() {
        assertThat(ADUserAccountControl.Flags.ACCOUNTDISABLE.isSetIn(1)).isFalse();
        assertThat(ADUserAccountControl.Flags.ACCOUNTDISABLE.isSetIn(2)).isTrue();
        assertThat(ADUserAccountControl.Flags.ACCOUNTDISABLE.isSetIn(66048)).isFalse();
        assertThat(ADUserAccountControl.Flags.ACCOUNTDISABLE.isSetIn(66050)).isTrue();

        assertThat(ADUserAccountControl.Flags.NORMAL_ACCOUNT.isSetIn(256)).isFalse();
        assertThat(ADUserAccountControl.Flags.NORMAL_ACCOUNT.isSetIn(512)).isTrue();
        assertThat(ADUserAccountControl.Flags.NORMAL_ACCOUNT.isSetIn(66048)).isTrue();
        assertThat(ADUserAccountControl.Flags.NORMAL_ACCOUNT.isSetIn(66050)).isTrue();
        assertThat(ADUserAccountControl.Flags.NORMAL_ACCOUNT.isSetIn(532480)).isFalse();
    }

    @Test
    void isAccountDisabled() {
        assertThat(ADUserAccountControl.create(1).accountIsDisabled()).isFalse();
        assertThat(ADUserAccountControl.create(2).accountIsDisabled()).isTrue();
        assertThat(ADUserAccountControl.create(66048).accountIsDisabled()).isFalse();
        assertThat(ADUserAccountControl.create(66050).accountIsDisabled()).isTrue();
    }

    @Test
    void isUserAccount() {
        assertThat(ADUserAccountControl.create(256).isUserAccount()).isFalse();
        assertThat(ADUserAccountControl.create(512).isUserAccount()).isTrue();
        assertThat(ADUserAccountControl.create(66048).isUserAccount()).isTrue();
        assertThat(ADUserAccountControl.create(66050).isUserAccount()).isTrue();
        assertThat(ADUserAccountControl.create(532480).isUserAccount()).isFalse();
    }

    @Test
    void printFlags() {
        ADUserAccountControl allFlagsAccountcontrol = ADUserAccountControl.create(
                        ADUserAccountControl.Flags.NORMAL_ACCOUNT.getFlagValue() |
                        ADUserAccountControl.Flags.ACCOUNTDISABLE.getFlagValue() |
                        ADUserAccountControl.Flags.PASSWORD_EXPIRED.getFlagValue());

        assertThat(allFlagsAccountcontrol.printFlags()).isEqualTo("ACCOUNTDISABLE|NORMAL_ACCOUNT|PASSWORD_EXPIRED");
    }
}
