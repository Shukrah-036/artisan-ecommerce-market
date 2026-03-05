package com.artisanmarket.product;

import com.artisanmarket.user.User;
import com.artisanmarket.user.UserRepository;
import com.artisanmarket.user.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

// product/ProductSeeder.java
@Component
@RequiredArgsConstructor
@Slf4j
public class ProductSeeder {
//    private final ProductRepository productRepository;
//    private final UserRepository userRepository;
//    private final RestTemplate restTemplate;
//
//    @Override
//    public void run(ApplicationArguments args) {
//        if (productRepository.count() > 0) return;
//        log.info("Seeding from Fake Store API...");
//
//        User seller = userRepository.save(User.builder()
//                .email("seed@artisanmarket.com").fullName("The Artisan Market")
//                .password("n/a").userRole(Role.ARTISAN).build());
//
//        FakeProduct[] products = restTemplate.getForObject(
//                "https://fakestoreapi.com/products", FakeProduct[].class);
//        if (products == null) return;
//
//        Arrays.stream(products).forEach(p ->
//                productRepository.save(Product.builder()
//                        .title(p.title()).description(p.description())
//                        .price(BigDecimal.valueOf(p.price()))
//                        .imageUrls(List.of(p.image()))
//                        .category(mapCategory(p.category()))
//                        .stockQuantity(ThreadLocalRandom.current().nextInt(5, 50))
//                        .seller(seller).isActive(true).build()));
//
//        log.info("Seeded {} products", products.length);
//    }
//
//    private ProductCategory mapCategory(String c) {
//        return switch (c.toLowerCase()) {
//            case "jewelery"    -> ProductCategory.HANDMADE;
//            case "electronics" -> ProductCategory.VINTAGE;
//            default            -> ProductCategory.ART;
//        };
//    }
//
//    record FakeProduct(String title, String description,
//                       double price, String image, String category) {}
}
