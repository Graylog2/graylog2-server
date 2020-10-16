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

/**
 * Active Directory UserAccountControl flags.
 *
 * See: https://docs.microsoft.com/en-us/troubleshoot/windows-server/identity/useraccountcontrol-manipulate-account-properties
 */
public enum ADUserAccountControlFlags {
    /**
     * The user account is disabled.
     */
    ACCOUNTDISABLE(0x2),

    /**
     * This is a default account type that represents a typical user. (not a computer, for example)
     */
    NORMAL_ACCOUNT(0x200);

    private final int flagValue;

    ADUserAccountControlFlags(int flagValue) {
        this.flagValue = flagValue;
    }

    public boolean isSetIn(int userAccountControlValue) {
        return (userAccountControlValue & flagValue) > 0;
    }

    public static boolean isAccountDisabled(int userAccountControlValue) {
        return ACCOUNTDISABLE.isSetIn(userAccountControlValue);
    }

    public static boolean isUserAccount(int userAccountControlValue) {
        return NORMAL_ACCOUNT.isSetIn(userAccountControlValue);
    }
}
