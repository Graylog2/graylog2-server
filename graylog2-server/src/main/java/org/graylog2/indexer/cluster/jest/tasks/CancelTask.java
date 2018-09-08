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
package org.graylog2.indexer.cluster.jest.tasks;

import io.searchbox.action.AbstractMultiINodeActionBuilder;
import io.searchbox.action.GenericResultAbstractAction;

import javax.annotation.Nonnull;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Objects;

public class CancelTask extends GenericResultAbstractAction {

  private final String taskId;

  public CancelTask(@Nonnull String taskId) {
    this.taskId = Objects.requireNonNull(taskId);
    setURI(buildURI());
  }

  @Override
  public String getRestMethodName() {
    return "POST";
  }

  @Override
  protected String buildURI() {
    try {
      return super.buildURI() + "/_tasks/" + URLEncoder.encode(taskId, "utf-8") + "/_cancel";
    } catch (UnsupportedEncodingException ignored) {
      // cannot happen, utf-8 is a system encoding
      return null;
    }
  }

  public static class Builder extends AbstractMultiINodeActionBuilder<CancelTask, Builder> {

    private String taskId;

    public Builder(@Nonnull String taskId) {
      Objects.requireNonNull(taskId);
      this.taskId = taskId;
    }

    public Builder(long id) {
      this(String.valueOf(id));
    }

    public Builder actions(String actionFilter) {
      return setParameter("actions", actionFilter);
    }

    @Override
    public CancelTask build() {
      return new CancelTask(taskId);
    }
  }
}
