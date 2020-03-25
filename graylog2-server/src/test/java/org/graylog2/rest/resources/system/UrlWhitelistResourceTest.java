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
package org.graylog2.rest.resources.system;

import org.graylog2.rest.models.system.urlwhitelist.WhitelistRegexGenerationRequest;
import org.graylog2.rest.models.system.urlwhitelist.WhitelistRegexGenerationResponse;
import org.graylog2.system.urlwhitelist.RegexHelper;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.assertj.core.api.Assertions.assertThat;

public class UrlWhitelistResourceTest {
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @InjectMocks
    UrlWhitelistResource urlWhitelistResource;

    @Spy
    RegexHelper regexHelper;

    @Test
    public void generateRegexForTemplate() {
        final WhitelistRegexGenerationRequest request =
                WhitelistRegexGenerationRequest.create("https://example.com/api/lookup?key=${key}", "${key}");
        final WhitelistRegexGenerationResponse response = urlWhitelistResource.generateRegex(request);
        assertThat(response.regex()).isNotBlank();
    }

    @Test
    public void generateRegexForUrl() {
        final WhitelistRegexGenerationRequest request =
                WhitelistRegexGenerationRequest.create("https://example.com/api/lookup", null);
        final WhitelistRegexGenerationResponse response = urlWhitelistResource.generateRegex(request);
        assertThat(response.regex()).isNotBlank();
    }
}
