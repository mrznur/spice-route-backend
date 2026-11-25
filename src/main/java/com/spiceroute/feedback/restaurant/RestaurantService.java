package com.spiceroute.feedback.restaurant;

import com.spiceroute.feedback.common.ApiException;
import com.spiceroute.feedback.domain.Restaurant;
import com.spiceroute.feedback.repository.RestaurantRepository;
import com.spiceroute.feedback.security.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RestaurantService {

    private final RestaurantRepository restaurants;

    public RestaurantService(RestaurantRepository restaurants) {
        this.restaurants = restaurants;
    }

    /** B6.4 — Owner fetches their own restaurant. */
    @Transactional(readOnly = true)
    public RestaurantResponse getMyRestaurant() {
        return RestaurantResponse.from(loadOwnerRestaurant());
    }

    /** B6.5 — Owner updates their restaurant name. Slug is not changed (would break existing QR links). */
    @Transactional
    public RestaurantResponse updateMyRestaurant(UpdateRestaurantRequest req) {
        Restaurant restaurant = loadOwnerRestaurant();
        restaurant.setName(req.name().trim());
        return RestaurantResponse.from(restaurant);
    }

    private Restaurant loadOwnerRestaurant() {
        return restaurants.findById(CurrentUser.restaurantId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Restaurant not found"));
    }
}
