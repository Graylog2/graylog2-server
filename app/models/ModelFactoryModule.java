/*
 * Copyright 2013 TORCH UG
 *
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package models;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import org.graylog2.restclient.models.*;
import org.graylog2.restclient.models.accounts.LdapSettings;
import org.graylog2.restclient.models.alerts.AlertCondition;
import org.graylog2.restclient.models.dashboards.Dashboard;

/**
 * Provides the bindings for the factories of our models to avoid having lots of static methods everywhere.
 */
public class ModelFactoryModule extends AbstractModule {
    @Override
    protected void configure() {
        install(new FactoryModuleBuilder().build(Node.Factory.class));
        install(new FactoryModuleBuilder().build(Input.Factory.class));
        install(new FactoryModuleBuilder().build(SystemJob.Factory.class));
        install(new FactoryModuleBuilder().build(UniversalSearch.Factory.class));
        install(new FactoryModuleBuilder().build(User.Factory.class));
        install(new FactoryModuleBuilder().build(Extractor.Factory.class));
        install(new FactoryModuleBuilder().build(Dashboard.Factory.class));
        install(new FactoryModuleBuilder().build(LdapSettings.Factory.class));
        install(new FactoryModuleBuilder().build(Stream.Factory.class));
        install(new FactoryModuleBuilder().build(StreamRule.Factory.class));
        install(new FactoryModuleBuilder().build(Radio.Factory.class));
        install(new FactoryModuleBuilder().build(Index.Factory.class));
        install(new FactoryModuleBuilder().build(SavedSearch.Factory.class));
        install(new FactoryModuleBuilder().build(AlertCondition.Factory.class));
        install(new FactoryModuleBuilder().build(InputState.Factory.class));
        install(new FactoryModuleBuilder().build(AlarmCallback.Factory.class));
        install(new FactoryModuleBuilder().build(Output.Factory.class));

        // TODO crutch, because we need the factory for systemjobs in all().
        // can this be done with a second factory for the list?
        // or possibly with a factory method returning List<SystemJob> ?
        requestStaticInjection(SystemJob.class);
    }
}
