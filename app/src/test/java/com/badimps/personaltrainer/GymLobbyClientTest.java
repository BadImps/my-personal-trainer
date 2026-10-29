package com.badimps.personaltrainer;

import static org.junit.Assert.assertEquals;


import org.junit.Test;

/** Regression coverage for the social actions shown in the Gym Lobby. */
public final class GymLobbyClientTest {
    @Test public void requestCopyExplainsEachLobbyAction() {
        assertEquals("Kai wants to become your Gym Buddy.",
                GymLobbyClient.requestDescription("buddy", "Kai", "Kai"));
        assertEquals("Kai invited you to Mia’s private lobby.",
                GymLobbyClient.requestDescription("invite", "Kai", "Mia"));
        assertEquals("Kai wants to join Mia’s private lobby.",
                GymLobbyClient.requestDescription("join", "Kai", "Mia"));
    }

    @Test public void leaderboardCallsOutThePodium() {
        assertEquals("1ST", GymLobbyClient.rankLabel(1));
        assertEquals("2ND", GymLobbyClient.rankLabel(2));
        assertEquals("3RD", GymLobbyClient.rankLabel(3));
        assertEquals("#4", GymLobbyClient.rankLabel(4));
    }

    @Test public void inboxCountIncludesPendingRequestsAndUnreadMessages() {
        assertEquals(2, GymLobbyClient.unreadCount(1, 1));
        assertEquals(0, GymLobbyClient.unreadCount(-1, -3));
    }
}
