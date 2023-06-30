package com.udacity.pricing;

import com.udacity.pricing.domain.price.Price;
import com.udacity.pricing.service.PriceException;
import com.udacity.pricing.service.PricingService;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class PricingServiceApplicationTests {

	@Test
	public void contextLoads() {
	}

	@Test
	public void testGetPrice() throws PriceException {
		// Arrange
		Long testId = 10L;

		// Act
		Price actualPrice = PricingService.getPrice(testId);

		// Assert
		Assertions.assertTrue(actualPrice.getPrice().doubleValue() > 0.0);
	}

	@Test
	public void testGetPriceInvalidId() throws PriceException {
		// Arrange
		Long testId = 200L;

		// Act & Assert
		Assertions.assertThrows(PriceException.class, () -> PricingService.getPrice(testId));
	}

}
