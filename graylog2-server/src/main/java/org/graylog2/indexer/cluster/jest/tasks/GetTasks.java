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

import javax.annotation.Nullable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class GetTasks extends GenericResultAbstractAction {

  @Nullable
  private final String taskId;

  protected GetTasks(Builder builder, @Nullable String taskId) {
    super(builder);
    this.taskId = taskId;
    setURI(buildURI());
  }

  @Override
  protected String buildURI() {
    final String baseUri = super.buildURI() + "/_tasks";
    if (taskId != null) {
      try {
        return baseUri + "/" + URLEncoder.encode(taskId, "utf-8");
      } catch (UnsupportedEncodingException ignored) {
        // cannot happen, utf-8 is a system encoding
        return null;
      }
    }
    return baseUri;
  }

  @Override
  public String getRestMethodName() {
    return "GET";
  }

  public static class Builder extends AbstractMultiINodeActionBuilder<GetTasks, GetTasks.Builder> {

    private String taskId;

    public Builder() {
      this(null);
    }

    public Builder(String taskId) {
      this.taskId = taskId;
    }

    public Builder waitForCompletion(boolean waitForCompletion) {
      return setParameter("wait_for_completion", waitForCompletion);
    }

    public Builder timeout(String timeout) {
      return setParameter("timeout", timeout);
    }

    public Builder actions(String actionFilter) {
      return setParameter("actions", actionFilter);
    }

    public Builder detailed(boolean detailed) {
      return addCleanApiParameter("detailed");
    }

    public Builder ofParentTaskId(String parentTaskId) {
      return setParameter("parent_task_id", parentTaskId);
    }

    @Override
    public GetTasks build() {
      final String joinedNodes = getJoinedNodes();
      // do not include `_all` if no nodes were given, some endpoints don't like this
      if (!"_all".equals(joinedNodes)) {
        // this api uses nodes as a parameter, not in the url
        setParameter("nodes", joinedNodes);
      }
      if (taskId != null) {
        // cannot request "detailed" for individual tasks, sigh :(
        removeCleanApiParameter("detailed");
      }
      return new GetTasks(this, taskId);
    }

  }

}
