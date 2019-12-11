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
package org.graylog2.lookup;

import com.google.inject.Scopes;
import org.graylog2.lookup.adapters.CSVFileDataAdapter;
import org.graylog2.lookup.adapters.DSVHTTPDataAdapter;
import org.graylog2.lookup.adapters.DnsLookupDataAdapter;
import org.graylog2.lookup.adapters.HTTPJSONPathDataAdapter;
import org.graylog2.lookup.caches.CaffeineLookupCache;
import org.graylog2.lookup.caches.NullCache;
import org.graylog2.plugin.inject.Graylog2Module;
import org.graylog2.system.urlwhitelist.UrlWhitelistService;

public class LookupModule extends Graylog2Module {

    @Override
    protected void configure() {
        serviceBinder().addBinding().to(UrlWhitelistService.class).in(Scopes.SINGLETON);
        serviceBinder().addBinding().to(LookupTableService.class).asEagerSingleton();

        installLookupCache(NullCache.NAME,
                NullCache.class,
                NullCache.Factory.class,
                NullCache.Config.class);

        installLookupCache(CaffeineLookupCache.NAME,
                CaffeineLookupCache.class,
                CaffeineLookupCache.Factory.class,
                CaffeineLookupCache.Config.class);

        installLookupDataAdapter(CSVFileDataAdapter.NAME,
                CSVFileDataAdapter.class,
                CSVFileDataAdapter.Factory.class,
                CSVFileDataAdapter.Config.class);

        installLookupDataAdapter2(DnsLookupDataAdapter.NAME,
                                 DnsLookupDataAdapter.class,
                                 DnsLookupDataAdapter.Factory.class,
                                 DnsLookupDataAdapter.Config.class);

        installLookupDataAdapter2(HTTPJSONPathDataAdapter.NAME,
                HTTPJSONPathDataAdapter.class,
                HTTPJSONPathDataAdapter.Factory.class,
                HTTPJSONPathDataAdapter.Config.class);

        installLookupDataAdapter(DSVHTTPDataAdapter.NAME,
                DSVHTTPDataAdapter.class,
                DSVHTTPDataAdapter.Factory.class,
                DSVHTTPDataAdapter.Config.class);
    }

}
