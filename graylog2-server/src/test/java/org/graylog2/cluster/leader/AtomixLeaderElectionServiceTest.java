package org.graylog2.cluster.leader;

import io.atomix.cluster.MemberId;
import io.atomix.cluster.discovery.MulticastDiscoveryProvider;
import io.atomix.core.Atomix;
import io.atomix.core.election.LeaderElection;
import io.atomix.core.election.Leadership;
import io.atomix.core.profile.Profile;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class AtomixLeaderElectionServiceTest {
    @Test
    void test() {
        final Atomix atomix = Atomix.builder()
                .withMemberId("a")
                .withAddress(13401)
                .withMembershipProvider(MulticastDiscoveryProvider.builder().build())
                .withProfiles(Profile.consensus("a"))
                .build();
        atomix.start().join();

        LeaderElection<MemberId> election = atomix.getLeaderElection("graylog-leader-election");
        Leadership<MemberId> leadership = election.run(atomix.getMembershipService().getLocalMember().id());
        assertThat(leadership.leader().id()).isEqualTo(atomix.getMembershipService().getLocalMember().id());

    }
}
