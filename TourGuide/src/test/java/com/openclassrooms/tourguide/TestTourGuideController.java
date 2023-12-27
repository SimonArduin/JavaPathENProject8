package com.openclassrooms.tourguide;

import com.openclassrooms.tourguide.helper.InternalTestHelper;
import com.openclassrooms.tourguide.service.RewardsService;
import com.openclassrooms.tourguide.service.TourGuideService;
import com.openclassrooms.tourguide.user.User;
import gpsUtil.GpsUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import rewardCentral.RewardCentral;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = TestTourGuideController.class)
public class TestTourGuideController {

    @Test
    public void getNearbyAttractions() throws JSONException {
        GpsUtil gpsUtil = new GpsUtil();
        RewardCentral rewardCentral = new RewardCentral();
        RewardsService rewardsService = new RewardsService(gpsUtil, rewardCentral);
        TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService);
        TourGuideController tourGuideController = new TourGuideController(tourGuideService, rewardsService, rewardCentral);
        InternalTestHelper.setInternalUserNumber(0);
        User user = new User(UUID.randomUUID(), "jon", "000", "jon@tourGuide.com");
        tourGuideService.addUser(user);

        JSONArray result = tourGuideController.getNearbyAttractions(user.getUserName());

        assertEquals(5, result.length());
        for(int i = 0; i < result.length(); i++) {
            String currentResult = result.get(i).toString();
            assertTrue(currentResult.contains("name"));
            assertTrue(currentResult.contains("lat"));
            assertTrue(currentResult.contains("long"));
            assertTrue(currentResult.contains("userLat"));
            assertTrue(currentResult.contains("userLong"));
            assertTrue(currentResult.contains("distance"));
            assertTrue(currentResult.contains("rewardPoints"));
        }
    }
}
