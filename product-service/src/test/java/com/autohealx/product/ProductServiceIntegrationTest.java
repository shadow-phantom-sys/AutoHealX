package com.autohealx.product;

import com.autohealx.product.service.ProductService;
import com.autohealx.shared.domain.Product;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@Testcontainers
@Transactional
class ProductServiceIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("autohealx_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.data.redis.host", () -> "localhost");
        registry.add("spring.data.redis.port", () -> "6379");
        registry.add("spring.cache.type", () -> "simple"); // Use simple cache for tests
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductService productService;

    @Autowired
    private ObjectMapper objectMapper;

    private Product testProduct;

    @BeforeEach
    void setUp() {
        testProduct = Product.builder()
                .name("Test Product")
                .description("A test product for integration testing")
                .price(new BigDecimal("99.99"))
                .stockQuantity(100)
                .category("Electronics")
                .imageUrl("https://example.com/image.jpg")
                .active(true)
                .build();
    }

    @Test
    void shouldCreateProduct() throws Exception {
        String productJson = objectMapper.writeValueAsString(testProduct);

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Test Product"))
                .andExpect(jsonPath("$.price").value(99.99))
                .andExpect(jsonPath("$.stockQuantity").value(100))
                .andExpect(jsonPath("$.category").value("Electronics"))
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    void shouldGetProductById() throws Exception {
        // Create product first
        Product createdProduct = productService.createProduct(testProduct);

        mockMvc.perform(get("/api/v1/products/{id}", createdProduct.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdProduct.getId()))
                .andExpect(jsonPath("$.name").value("Test Product"))
                .andExpect(jsonPath("$.price").value(99.99));
    }

    @Test
    void shouldReturnNotFoundForNonExistentProduct() throws Exception {
        mockMvc.perform(get("/api/v1/products/{id}", 99999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Product not found with id: '99999'"));
    }

    @Test
    void shouldUpdateProduct() throws Exception {
        // Create product first
        Product createdProduct = productService.createProduct(testProduct);

        // Update the product
        createdProduct.setName("Updated Product");
        createdProduct.setPrice(new BigDecimal("149.99"));

        String updatedProductJson = objectMapper.writeValueAsString(createdProduct);

        mockMvc.perform(put("/api/v1/products/{id}", createdProduct.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatedProductJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Product"))
                .andExpect(jsonPath("$.price").value(149.99));
    }

    @Test
    void shouldDeleteProduct() throws Exception {
        // Create product first
        Product createdProduct = productService.createProduct(testProduct);

        mockMvc.perform(delete("/api/v1/products/{id}", createdProduct.getId()))
                .andExpect(status().isNoContent());

        // Verify product is soft deleted (not found when searching for active products)
        mockMvc.perform(get("/api/v1/products/{id}", createdProduct.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReduceStock() throws Exception {
        // Create product first
        Product createdProduct = productService.createProduct(testProduct);

        String stockRequest = "{\"quantity\": 10}";

        mockMvc.perform(post("/api/v1/products/{id}/stock/reduce", createdProduct.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(stockRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.quantityReduced").value(10));

        // Verify stock was reduced
        mockMvc.perform(get("/api/v1/products/{id}", createdProduct.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockQuantity").value(90));
    }

    @Test
    void shouldIncreaseStock() throws Exception {
        // Create product first
        Product createdProduct = productService.createProduct(testProduct);

        String stockRequest = "{\"quantity\": 20}";

        mockMvc.perform(post("/api/v1/products/{id}/stock/increase", createdProduct.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(stockRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.quantityIncreased").value(20));

        // Verify stock was increased
        mockMvc.perform(get("/api/v1/products/{id}", createdProduct.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockQuantity").value(120));
    }

    @Test
    void shouldCheckStockAvailability() throws Exception {
        // Create product first
        Product createdProduct = productService.createProduct(testProduct);

        mockMvc.perform(get("/api/v1/products/{id}/stock/check", createdProduct.getId())
                        .param("quantity", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true))
                .andExpect(jsonPath("$.requiredQuantity").value(50));

        mockMvc.perform(get("/api/v1/products/{id}/stock/check", createdProduct.getId())
                        .param("quantity", "150"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false))
                .andExpect(jsonPath("$.requiredQuantity").value(150));
    }

    @Test
    void shouldGetAllProducts() throws Exception {
        // Create multiple products
        productService.createProduct(testProduct);
        
        Product product2 = Product.builder()
                .name("Another Product")
                .description("Another test product")
                .price(new BigDecimal("49.99"))
                .stockQuantity(50)
                .category("Books")
                .active(true)
                .build();
        productService.createProduct(product2);

        mockMvc.perform(get("/api/v1/products")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void shouldSearchProductsByName() throws Exception {
        // Create product first
        productService.createProduct(testProduct);

        mockMvc.perform(get("/api/v1/products")
                        .param("search", "Test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].name").value("Test Product"));
    }

    @Test
    void shouldFilterProductsByCategory() throws Exception {
        // Create product first
        productService.createProduct(testProduct);

        mockMvc.perform(get("/api/v1/products")
                        .param("category", "Electronics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].category").value("Electronics"));
    }

    @Test
    void shouldGetLowStockProducts() throws Exception {
        // Create product with low stock
        Product lowStockProduct = Product.builder()
                .name("Low Stock Product")
                .description("A product with low stock")
                .price(new BigDecimal("29.99"))
                .stockQuantity(5)
                .category("Books")
                .active(true)
                .build();
        productService.createProduct(lowStockProduct);

        mockMvc.perform(get("/api/v1/products/low-stock")
                        .param("threshold", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].name").value("Low Stock Product"));
    }

    @Test
    void shouldGetCategories() throws Exception {
        // Create products with different categories
        productService.createProduct(testProduct); // Electronics
        
        Product bookProduct = Product.builder()
                .name("Book Product")
                .description("A book")
                .price(new BigDecimal("19.99"))
                .stockQuantity(30)
                .category("Books")
                .active(true)
                .build();
        productService.createProduct(bookProduct);

        mockMvc.perform(get("/api/v1/products/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void shouldValidateProductCreation() throws Exception {
        Product invalidProduct = Product.builder()
                .name("") // Invalid: empty name
                .price(new BigDecimal("-10.00")) // Invalid: negative price
                .stockQuantity(-5) // Invalid: negative stock
                .build();

        String productJson = objectMapper.writeValueAsString(invalidProduct);

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }
}