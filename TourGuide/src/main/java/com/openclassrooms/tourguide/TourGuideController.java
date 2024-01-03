package com.openclassrooms.tourguide;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import com.openclassrooms.tourguide.service.RewardsService;
import gpsUtil.location.Location;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import gpsUtil.location.Attraction;
import gpsUtil.location.VisitedLocation;

import com.openclassrooms.tourguide.service.TourGuideService;
import com.openclassrooms.tourguide.user.User;
import com.openclassrooms.tourguide.user.UserReward;

import rewardCentral.RewardCentral;
import tripPricer.Provider;

@RestController
public class TourGuideController {

	@Autowired
	TourGuideService tourGuideService;

    @Autowired
    RewardsService rewardsService;

    @Autowired
    RewardCentral rewardCentral;

    public TourGuideController(TourGuideService tourGuideService, RewardsService rewardsService, RewardCentral rewardCentral) {
        this.tourGuideService = tourGuideService;
        this.rewardsService = rewardsService;
        this.rewardCentral = rewardCentral;
    }
	
    @RequestMapping("/")
    public String index() {
        return "Greetings from TourGuide!";
    }
    
    @RequestMapping("/getLocation") 
    public VisitedLocation getLocation(@RequestParam String userName) throws ExecutionException, InterruptedException {
    	return tourGuideService.getUserLocation(getUser(userName));
    }
    
    //  TODO: Change this method to no longer return a List of Attractions.
 	//  Instead: Get the closest five tourist attractions to the user - no matter how far away they are.
 	//  Return a new JSON object that contains:
    	// Name of Tourist attraction, 
        // Tourist attractions lat/long, 
        // The user's location lat/long, 
        // The distance in miles between the user's location and each of the attractions.
        // The reward points for visiting each Attraction.
        //    Note: Attraction reward points can be gathered from RewardsCentral
    @RequestMapping("/getNearbyAttractions") 
    public JSONArray getNearbyAttractions(@RequestParam String userName) throws ExecutionException, InterruptedException {
        User user = getUser(userName);
    	VisitedLocation visitedLocation = tourGuideService.getUserLocation(user);
    	List<Attraction> nearByAttractions = tourGuideService.getNearByAttractions(visitedLocation);
        JSONArray result = new JSONArray();
        for(Attraction attraction : nearByAttractions) {
            Map<String, String> map = new HashMap<>();
            map.put("name", attraction.attractionName);
            map.put("lat", String.valueOf(attraction.latitude));
            map.put("long", String.valueOf(attraction.longitude));
            map.put("userLat", String.valueOf(visitedLocation.location.latitude));
            map.put("userLong", String.valueOf(visitedLocation.location.longitude));
            map.put("distance", String.valueOf(rewardsService.getDistance(visitedLocation.location, new Location(attraction.latitude, attraction.longitude))));
            map.put("rewardPoints", String.valueOf(rewardCentral.getAttractionRewardPoints(attraction.attractionId, user.getUserId())));
            JSONObject attractionJson = new JSONObject(map);
            result.put(attractionJson);
        }
        return result;
    }
    
    @RequestMapping("/getRewards") 
    public List<UserReward> getRewards(@RequestParam String userName) {
    	return tourGuideService.getUserRewards(getUser(userName));
    }
       
    @RequestMapping("/getTripDeals")
    public List<Provider> getTripDeals(@RequestParam String userName) {
    	return tourGuideService.getTripDeals(getUser(userName));
    }
    
    private User getUser(String userName) {
    	return tourGuideService.getUser(userName);
    }
   

}