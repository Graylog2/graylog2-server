package org.graylog.plugins.views.search.views.dynamicstartpage;

import com.google.common.eventbus.Subscribe;
import org.graylog.security.events.EntitySharesUpdateEvent;

import javax.inject.Inject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

public class RecentActivityUpdatesListener {
    private final RecentActivityService recentActivityService;

    @Inject
    public RecentActivityUpdatesListener(RecentActivityService recentActivityService) {
        this.recentActivityService = recentActivityService;
    }

    @Subscribe
    public void createRecentActivityFor(final RecentActivityEvent event) {
        recentActivityService.save(RecentActivityDTO.builder()
                .activityType(event.activityType())
                .itemId(event.itemId())
                .itemType(event.itemType())
                .itemTitle(event.itemTitle())
                .build());
    }

    private <T> Predicate<T> distinctByKey(
            Function<? super T, ?> keyExtractor) {

        Map<Object, Boolean> seen = new ConcurrentHashMap<>();
        return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }

    @Subscribe
    public void createRecentActivityFor(final EntitySharesUpdateEvent event) {
        event.creates().stream().filter(distinctByKey(EntitySharesUpdateEvent.Share::grantee))
                .forEach(e -> recentActivityService.save(RecentActivityDTO.builder()
                        .activityType(ActivityType.SHARED)
                        .itemId(event.entity().entity())
                        .userName(event.user().getFullName())
                        .grantee(e.grantee().toString())
                        .build())
                );

        event.deletes().stream().filter(distinctByKey(EntitySharesUpdateEvent.Share::grantee))
                .forEach(e -> recentActivityService.save(RecentActivityDTO.builder()
                        .activityType(ActivityType.UNSHARED)
                        .itemId(event.entity().entity())
                        .userName(event.user().getFullName())
                        .grantee(e.grantee().toString())
                        .build()) );
    }

}
