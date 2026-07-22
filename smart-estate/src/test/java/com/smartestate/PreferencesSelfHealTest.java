package com.smartestate;

import com.smartestate.model.*;
import com.smartestate.repo.NotificationPreferenceRepository;
import com.smartestate.repo.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the resident preferences page self-heals: a resident with no NotificationPreference rows
 * (e.g. created outside the seeder) gets a default row per non-emergency category on first open, so
 * the Communication Preferences table is never empty.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PreferencesSelfHealTest {

    @Autowired MockMvc mvc;
    @Autowired UserRepository userRepo;
    @Autowired NotificationPreferenceRepository prefRepo;

    @Test
    void openingPreferencesCreatesDefaultsWhenMissing() throws Exception {
        User u = new User();
        u.setName("No Prefs Resident");
        u.setEmail("noprefs@smartestate.hk");
        u.setPasswordHash("x"); // not used; we authenticate via a mock user
        u.setRole(Role.RESIDENT);
        u.setLanguage(Language.EN);
        u = userRepo.save(u);

        assertTrue(prefRepo.findByUserOrderByCategoryAsc(u).isEmpty(), "resident starts with no preferences");

        mvc.perform(get("/resident/preferences").with(user(u.getEmail()).roles("RESIDENT")))
                .andExpect(status().isOk());

        List<NotificationPreference> created = prefRepo.findByUserOrderByCategoryAsc(u);
        long expected = Arrays.stream(NoticeCategory.values()).filter(c -> !c.isForcedAllChannels()).count();
        assertEquals(expected, created.size(), "one preference row per non-emergency category");
        assertTrue(created.stream().noneMatch(p -> p.getCategory().isForcedAllChannels()),
                "emergency category is excluded (it always forces all channels)");
    }
}
