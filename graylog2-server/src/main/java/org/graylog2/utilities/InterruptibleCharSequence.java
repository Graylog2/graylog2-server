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
package org.graylog2.utilities;

public class InterruptibleCharSequence implements CharSequence {
    private CharSequence inner;

    public InterruptibleCharSequence(CharSequence inner) {
        super();
        this.inner = inner;
    }

    @Override
    public char charAt(int index) {
        if (Thread.interrupted()) { // clears flag if set
            throw new RuntimeException(new InterruptedException());
        }
        return inner.charAt(index);
    }

    @Override
    public int length() {
        return inner.length();
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return new InterruptibleCharSequence(inner.subSequence(start, end));
    }

    @Override
    public String toString() {
        return inner.toString();
    }
}
