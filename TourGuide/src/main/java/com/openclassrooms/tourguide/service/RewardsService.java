package com.openclassrooms.tourguide.service;

import java.awt.geom.Point2D;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import rewardCentral.RewardCentral;
import com.openclassrooms.tourguide.user.User;
import com.openclassrooms.tourguide.user.UserReward;

@Service
public class RewardsService {
	private static final double STATUTE_MILES_PER_NAUTICAL_MILE = 1.15077945;

	// proximity in miles
	private int defaultProximityBuffer = 10;
	private int proximityBuffer = defaultProximityBuffer;
	private int attractionProximityRange = 200;
	private final GpsUtil gpsUtil;
	private final RewardCentral rewardsCentral;
	public final ExecutorService executorService;

	public RewardsService(GpsUtil gpsUtil, RewardCentral rewardCentral) {
		this.gpsUtil = gpsUtil;
		this.rewardsCentral = rewardCentral;
		this.executorService = Executors.newFixedThreadPool(50);
	}

	public void setProximityBuffer(int proximityBuffer) {
		this.proximityBuffer = proximityBuffer;
	}

	public void setDefaultProximityBuffer() {
		proximityBuffer = defaultProximityBuffer;
	}

	public Future<?> calculateRewards(User user) {
		CopyOnWriteArrayList<VisitedLocation> userLocations = new CopyOnWriteArrayList<>(user.getVisitedLocations());
		List<Attraction> attractions = gpsUtil.getAttractions();

		return executorService.submit( () -> {
			for(VisitedLocation visitedLocation : userLocations) {

				for(Attraction attraction : attractions) {
					if(user.getUserRewards().stream().filter(r -> r.attraction.attractionName.equals(attraction.attractionName)).count() == 0) {
						if(nearAttraction(visitedLocation, attraction)) {
							user.addUserReward(new UserReward(visitedLocation, attraction, getRewardPoints(attraction, user)));
						}
					}
				}
			}
		});
	}

	/*public void calculateRewards(User user) throws ExecutionException, InterruptedException {
		CopyOnWriteArrayList<VisitedLocation> userLocations = new CopyOnWriteArrayList<>(user.getVisitedLocations());
		List<Attraction> attractions = gpsUtil.getAttractions();
		CompletableFuture<Object> result = null;

		for(VisitedLocation visitedLocation : userLocations) {
			for(Attraction attraction : attractions) {

				// CompletableFuture.supplyAsync(UserReward)
				CompletableFuture<Attraction> cf1 = CompletableFuture.supplyAsync(() -> {
					if (user.getUserRewards().stream().filter(r -> r.attraction.attractionName.equals(attraction.attractionName)).count() == 0)
						return attraction;
					else
						return null;
				}, executorService);

				// CompletableFuture.supplyAsync(nearAttraction)
				CompletableFuture<VisitedLocation> cf2 = CompletableFuture.supplyAsync(() -> {
					if (nearAttraction(visitedLocation, attraction))
						return visitedLocation;
					else
						return null;
				}, executorService);

				// if(both not null) CompletableFuture(addUserReward)
				result = CompletableFuture.allOf(cf1, cf2).thenApplyAsync(ignored -> {
					try {
						if (cf1.get() == null || cf2.get() == null) {
							return null;
						}
						user.addUserReward(new UserReward(cf2.get(), cf1.get(), getRewardPoints(cf1.get(), user)));
					} catch (InterruptedException e) {
						throw new RuntimeException(e);
					} catch (ExecutionException e) {
						throw new RuntimeException(e);
					}
					return null;
				}, executorService);
			}
		};
	}*/

	/*public void calculateRewards(User user) throws ExecutionException, InterruptedException {
		CopyOnWriteArrayList<VisitedLocation> userLocations = new CopyOnWriteArrayList<>(user.getVisitedLocations());
		List<Attraction> attractions = gpsUtil.getAttractions();

		for(VisitedLocation visitedLocation : userLocations) {
			for(Attraction attraction : attractions) {

				CompletableFuture.runAsync(() -> {
					if (user.getUserRewards().stream().noneMatch(r -> r.attraction.attractionName.equals(attraction.attractionName)))
						if (nearAttraction(visitedLocation, attraction))
							user.addUserReward(new UserReward(visitedLocation, attraction, getRewardPoints(attraction, user)));
				}, executorService);
			}
		};
	}*/

	public boolean isWithinAttractionProximity(Attraction attraction, Location location) {
		return getDistance(attraction, location) > attractionProximityRange ? false : true;
	}

	private boolean nearAttraction(VisitedLocation visitedLocation, Attraction attraction) {
		return getDistance(attraction, visitedLocation.location) > proximityBuffer ? false : true;
	}

	private int getRewardPoints(Attraction attraction, User user) {
		return rewardsCentral.getAttractionRewardPoints(attraction.attractionId, user.getUserId());
	}

	public double getDistance(Location loc1, Location loc2) {
		double lat1 = Math.toRadians(loc1.latitude);
		double lon1 = Math.toRadians(loc1.longitude);
		double lat2 = Math.toRadians(loc2.latitude);
		double lon2 = Math.toRadians(loc2.longitude);

		double angle = Math.acos(Math.sin(lat1) * Math.sin(lat2)
				+ Math.cos(lat1) * Math.cos(lat2) * Math.cos(lon1 - lon2));

		double nauticalMiles = 60 * Math.toDegrees(angle);
		double statuteMiles = STATUTE_MILES_PER_NAUTICAL_MILE * nauticalMiles;
		return statuteMiles;
	}
}
