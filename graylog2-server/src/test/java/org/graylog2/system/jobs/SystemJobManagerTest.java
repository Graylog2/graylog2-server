/**
 * Copyright 2013 Lennart Koopmann <lennart@torch.sh>
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
 *
 */
package org.graylog2.system.jobs;

import org.graylog2.GraylogServerStub;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class SystemJobManagerTest {

    @Test
    public void testGetRunningJobs() throws Exception {
        SystemJobManager manager = new SystemJobManager(new GraylogServerStub());

        LongRunningJob job1 = new LongRunningJob(1);
        LongRunningJob job2 = new LongRunningJob(1);

        String jobID1 = manager.submit(job1);
        String jobID2 = manager.submit(job2);

        assertEquals(2, manager.getRunningJobs().size());
        assertTrue(manager.getRunningJobs().containsValue(job1));
        assertTrue(manager.getRunningJobs().containsValue(job2));

        assertEquals(jobID1, manager.getRunningJobs().get(jobID1).getId());
        assertEquals(jobID2, manager.getRunningJobs().get(jobID2).getId());
    }

    @Test
    public void testConcurrentJobs() throws Exception {
        SystemJobManager manager = new SystemJobManager(new GraylogServerStub());

        SystemJob job1 = new LongRunningJob(3);
        SystemJob job2 = new LongRunningJob(3);
        SystemJob job3 = new AnotherLongRunningJob(3);

        manager.submit(job1);
        manager.submit(job2);
        manager.submit(job3);

        assertEquals(3, manager.getRunningJobs().size());
        assertEquals(2, manager.concurrentJobs(job1.getClass()));
    }

    @Test
    public void testSubmitThrowsExceptionIfMaxConcurrencyLevelReached() throws Exception {
        SystemJobManager manager = new SystemJobManager(new GraylogServerStub());

        LongRunningJob job1 = new LongRunningJob(3);
        LongRunningJob job2 = new LongRunningJob(3);
        SystemJob job3 = new AnotherLongRunningJob(3);

        // We have to set it for both instances in tests because the stubs are dynamic and no static max level can be set.
        job1.setMaxConcurrency(1);
        job2.setMaxConcurrency(1);

        manager.submit(job1);

        boolean exceptionThrown = false;
        try {
            manager.submit(job2);
        } catch(SystemJobConcurrencyException e) {
            exceptionThrown = true;
        }

        assertTrue(exceptionThrown);

        manager.submit(job3);

        assertEquals(2, manager.getRunningJobs().size());
        assertEquals(1, manager.concurrentJobs(job1.getClass()));
    }

    class LongRunningJob extends SystemJob {

        private int seconds;
        private int maxConcurrency = 9001;

        public LongRunningJob(int seconds) {
            this.seconds = seconds;
        }

        @Override
        public void execute() {
            try {
                Thread.sleep(seconds*1000);
            } catch (InterruptedException e) {
                // That's fine.
                return;
            }
        }

        void setMaxConcurrency(int maxConcurrency) {
            this.maxConcurrency = maxConcurrency;
        }

        @Override
        public void requestCancel() {
        }

        @Override
        public int getProgress() {
            return 0;
        }

        @Override
        public int maxConcurrency() {
            return maxConcurrency;
        }

        @Override
        public boolean providesProgress() {
            return false;
        }

        @Override
        public boolean isCancelable() {
            return false;
        }

        @Override
        public String getDescription() {
            return "Test Job. You better not use this anywhere else, bro.";
        }

        @Override
        public String getClassName() {
            return getClass().getCanonicalName();
        }
    }

    class AnotherLongRunningJob extends SystemJob {

        private int seconds;

        public AnotherLongRunningJob(int seconds) {
            this.seconds = seconds;
        }

        @Override
        public void execute() {
            try {
                Thread.sleep(seconds*1000);
            } catch (InterruptedException e) {
                // That's fine.
                return;
            }
        }

        @Override
        public void requestCancel() {
        }

        @Override
        public int getProgress() {
            return 0;
        }

        @Override
        public int maxConcurrency() {
            return 9001;
        }

        @Override
        public boolean providesProgress() {
            return false;
        }

        @Override
        public boolean isCancelable() {
            return false;
        }

        @Override
        public String getDescription() {
            return "Another Test Job. You better not use this anywhere else, bro.";
        }

        @Override
        public String getClassName() {
            return getClass().getCanonicalName();
        }
    }
}
