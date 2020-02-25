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
package org.graylog2.security.token;

import com.google.common.base.Preconditions;
import com.google.common.hash.Hashing;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.graylog2.Configuration;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.cluster.ClusterId;
import org.graylog2.security.AESTools;

import javax.inject.Inject;
import java.nio.charset.StandardCharsets;

public class AccessTokenCipher {

    private final byte[] iV;
    private final String encryptionKey;

    @Inject
    public AccessTokenCipher(ClusterConfigService clusterConfigService, Configuration configuration) {
        encryptionKey = configuration.getPasswordSecret()
                .substring(0, 16);

        ClusterId clusterId = clusterConfigService.get(ClusterId.class);
        Preconditions.checkArgument(clusterId != null && StringUtils.isNotBlank(clusterId.clusterId()),
                "No cluster ID found. This is a precondition for being able to encrypt/decrypt access tokens!");

        // Hash the cluster ID so that we always get the 16 bytes we need for the IV
        final byte[] hashedClusterId = Hashing.sha256()
                .hashString(clusterId.clusterId(), StandardCharsets.UTF_8)
                .asBytes();

        this.iV = ArrayUtils.subarray(hashedClusterId, 0, 16);
    }

    public String encrypt(String cleartext) {
        return AESTools.encrypt(cleartext, encryptionKey, iV);
    }

    public String decrypt(String ciphertext) {
        return AESTools.decrypt(ciphertext, encryptionKey, iV);
    }
}
