package com.spiceroute.feedback.restaurant;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Restaurant", description = "Owner's restaurant settings")
@RestController
@RequestMapping("/api/restaurants")
public class RestaurantController {

    private final RestaurantService service;

    public RestaurantController(RestaurantService service) {
        this.service = service;
    }

    /** B6.4 */
    @GetMapping("/me")
    @PreAuthorize("hasRole('OWNER')")
    public RestaurantResponse getMyRestaurant() {
        return service.getMyRestaurant();
    }

    /** B6.5 */
    @PutMapping("/me")
    @PreAuthorize("hasRole('OWNER')")
    public RestaurantResponse updateMyRestaurant(@Valid @RequestBody UpdateRestaurantRequest req) {
        return service.updateMyRestaurant(req);
    }
}
