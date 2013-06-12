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
package org.graylog2.systemjobs;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class SystemJobReference {

    private boolean isComplete = false;
    private int percentDone = 0;

    public SystemJobReference(SystemJob job) {

    }

    public void setPercentDone(int percent) throws IllegalArgumentException {
        if (percent < 0 || percent > 100) {
            throw new IllegalArgumentException("Percent cannot be lower than 0 or higher than 100");
        }

        if (percent == 100) {
            isComplete();
        }
    }

    public int getPercentDone() {
        return percentDone;
    }

    public void setComplete() {
        isComplete = true;
    }

    public boolean isComplete() {
        return isComplete();
    }

}
