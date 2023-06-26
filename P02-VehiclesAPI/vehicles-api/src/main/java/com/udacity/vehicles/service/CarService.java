package com.udacity.vehicles.service;

import com.udacity.vehicles.client.maps.Address;
import com.udacity.vehicles.client.prices.Price;
import com.udacity.vehicles.domain.Location;
import com.udacity.vehicles.domain.car.Car;
import com.udacity.vehicles.domain.car.CarRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Implements the car service create, read, update or delete
 * information about vehicles, as well as gather related
 * location and price data when desired.
 */
@Service
public class CarService {
    private final CarRepository repository;

    @Value("${maps.endpoint}")
    private String mapsUrl;

    @Value("${pricing.endpoint}")
    private String pricingUrl;
    private final String PRICE_SERVICE_API_ENDPOINT = "services/price";
    private final String MAP_SERVICE_API_ENDPOINT = "/maps";
    private final WebClient webClientPricing;
    private final WebClient webClientMaps;

    public CarService(CarRepository repository, @Qualifier("pricing") WebClient webClientPricing, @Qualifier("maps") WebClient webClientMaps) {
        this.repository = repository;
        this.webClientPricing = webClientPricing;
        this.webClientMaps = webClientMaps;
    }

    /**
     * Gathers a list of all vehicles
     * @return a list of all vehicles in the CarRepository
     */
    public List<Car> list() {
        return repository.findAll();
    }

    /**
     * Gets car information by ID (or throws exception if non-existent)
     * @param id the ID number of the car to gather information on
     * @return the requested car's information, including location and price
     */
    public Car findById(Long id) {
        Optional<Car> car = this.repository.findById(id);
        if(car.isEmpty()) {
            throw new CarNotFoundException();
        }

        car.get().setPrice(getPriceForCar(car.get()));

        car.get().setLocation(getLocationForCar(car.get()));

        return car.get();
    }

    private String getPriceForCar(Car car) {
        String priceServiceUrl = pricingUrl + PRICE_SERVICE_API_ENDPOINT;
        priceServiceUrl = UriComponentsBuilder.fromUriString(priceServiceUrl)
                .queryParam("vehicleId", car.getId().toString())
                .toUriString();
        Price price = webClientPricing.get()
                .uri(priceServiceUrl)
                .retrieve()
                .bodyToMono(Price.class)
                .block();

        return (price.getPrice().toString());
    }

    private Location getLocationForCar(Car car) {
        String mapServiceUrl = mapsUrl + MAP_SERVICE_API_ENDPOINT;
        mapServiceUrl = UriComponentsBuilder.fromUriString(mapServiceUrl)
                .queryParam("lat", car.getLocation().getLat())
                .queryParam("lon", car.getLocation().getLon())
                .toUriString();
        Address address = webClientMaps.get()
                .uri(mapServiceUrl)
                .retrieve()
                .bodyToMono(Address.class)
                .block();

        Location location = new Location(car.getLocation().getLat(), car.getLocation().getLon());
        location.setAddress(address.getAddress());
        location.setState(address.getState());
        location.setCity(address.getCity());
        location.setZip(address.getZip());

        return location;
    }

    /**
     * Either creates or updates a vehicle, based on prior existence of car
     * @param car A car object, which can be either new or existing
     * @return the new/updated car is stored in the repository
     */
    public Car save(Car car) {
        if (car.getId() != null) {
            return repository.findById(car.getId())
                    .map(carToBeUpdated -> {
                        carToBeUpdated.setDetails(car.getDetails());
                        carToBeUpdated.setLocation(car.getLocation());
                        return repository.save(carToBeUpdated);
                    }).orElseThrow(CarNotFoundException::new);
        }

        return repository.save(car);
    }

    /**
     * Deletes a given car by ID
     * @param id the ID number of the car to delete
     */
    public void delete(Long id) {
        Optional<Car> car = this.repository.findById(id);
        if(car.isEmpty()) {
            throw new CarNotFoundException();
        }

        this.repository.delete(car.get());


    }

    private String getParamsString(Map<String, String> parameters)
            throws UnsupportedEncodingException {
        StringBuilder result = new StringBuilder();

        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            result.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
            result.append("=");
            result.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
            result.append("&");
        }

        String resultString = result.toString();
        return resultString.length() > 0
                ? resultString.substring(0, resultString.length() - 1)
                : resultString;
    }
}
