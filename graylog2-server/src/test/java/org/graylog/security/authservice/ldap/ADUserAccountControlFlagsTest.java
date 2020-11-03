/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.security.authservice.ldap;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ADUserAccountControlFlagsTest {
    @Test
    void isSetIn() {
        assertThat(ADUserAccountControlFlags.ACCOUNTDISABLE.isSetIn(1)).isFalse();
        assertThat(ADUserAccountControlFlags.ACCOUNTDISABLE.isSetIn(2)).isTrue();
        assertThat(ADUserAccountControlFlags.ACCOUNTDISABLE.isSetIn(66048)).isFalse();
        assertThat(ADUserAccountControlFlags.ACCOUNTDISABLE.isSetIn(66050)).isTrue();

        assertThat(ADUserAccountControlFlags.NORMAL_ACCOUNT.isSetIn(256)).isFalse();
        assertThat(ADUserAccountControlFlags.NORMAL_ACCOUNT.isSetIn(512)).isTrue();
        assertThat(ADUserAccountControlFlags.NORMAL_ACCOUNT.isSetIn(66048)).isTrue();
        assertThat(ADUserAccountControlFlags.NORMAL_ACCOUNT.isSetIn(66050)).isTrue();
        assertThat(ADUserAccountControlFlags.NORMAL_ACCOUNT.isSetIn(532480)).isFalse();
    }

    @Test
    void isAccountDisabled() {
        assertThat(ADUserAccountControlFlags.isAccountDisabled(1)).isFalse();
        assertThat(ADUserAccountControlFlags.isAccountDisabled(2)).isTrue();
        assertThat(ADUserAccountControlFlags.isAccountDisabled(66048)).isFalse();
        assertThat(ADUserAccountControlFlags.isAccountDisabled(66050)).isTrue();
    }

    @Test
    void isUserAccount() {
        assertThat(ADUserAccountControlFlags.isUserAccount(256)).isFalse();
        assertThat(ADUserAccountControlFlags.isUserAccount(512)).isTrue();
        assertThat(ADUserAccountControlFlags.isUserAccount(66048)).isTrue();
        assertThat(ADUserAccountControlFlags.isUserAccount(66050)).isTrue();
        assertThat(ADUserAccountControlFlags.isUserAccount(532480)).isFalse();
    }
}
