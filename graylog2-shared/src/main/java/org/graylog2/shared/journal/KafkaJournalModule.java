/*
 * Copyright 2014 TORCH GmbH
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
package org.graylog2.shared.journal;

import com.google.common.util.concurrent.Service;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;
import org.graylog2.plugin.inject.Graylog2Module;

import java.util.concurrent.ScheduledExecutorService;

public class KafkaJournalModule extends Graylog2Module {
    @Override
    protected void configure() {
        final Multibinder<Service> serviceBinder = Multibinder.newSetBinder(binder(), Service.class);
        serviceBinder.addBinding().to(JournalReader.class).in(Scopes.SINGLETON);
        serviceBinder.addBinding().to(KafkaJournal.class);
        bind(Journal.class).to(KafkaJournal.class);
    }

    @Provides
    @Singleton
    public KafkaJournal providesJournal(@Named("journalDirectory") String journalDirName, @Named("scheduler") ScheduledExecutorService scheduler) {
        return new KafkaJournal(journalDirName, scheduler);
    }
}
