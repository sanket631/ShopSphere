package com.backend.ShopSphere.service;

import com.backend.ShopSphere.dto.*;
import com.backend.ShopSphere.entity.*;
import com.backend.ShopSphere.exception.ConflictException;
import com.backend.ShopSphere.exception.IllegalArgsException;
import com.backend.ShopSphere.exception.ResourceNotFoundException;
import com.backend.ShopSphere.mapper.ProductAndCategoryMapper;
import com.backend.ShopSphere.repository.*;
import com.backend.ShopSphere.security.SecurityUtil;
import com.backend.ShopSphere.specification.ProductSpecification;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final SecurityUtil securityUtil;
    private final FileUploadService fileUploadService;
    private final ProductImageRepository productImageRepository;
    private final ProductAndCategoryMapper productAndCategoryMapper;

    @Value("${shopsphere.order.return-window-days}")
    private int returnWindowDays;

    @Transactional
    @CacheEvict(
            value = {
                    CacheConstants.PRODUCTS,
                    CacheConstants.PRODUCT_VARIANTS
            },
            allEntries = true
    )
    public ProductResponse createProduct(ProductRequest request) {

        log.info("Creating new product '{}'", request.getName());

        validateProductRequest(request);

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> {
                    log.error("Category not found. categoryId={}", request.getCategoryId());
                    throw new ResourceNotFoundException("Category not found");
                });

        User seller = getCurrentSeller();

        Product product = Product.builder()
                .name(request.getName())
                .slug(request.getSlug())
                .brand(request.getBrand())
                .shortDescription(request.getShortDescription())
                .longDescription(request.getLongDescription())
                .returnApplicable(request.getIsReturnApplicable())
                .returnWindowDays(request.getReturnWindowDays() != null ? request.getReturnWindowDays() : returnWindowDays)
                .category(category)
                .seller(seller)
                .build();

        List<ProductVariant> variants = createProductVariants(
                request.getVariants(),
                product
        );

        product.setVariants(variants);

        Product saved = productRepository.save(product);

        log.info("Product created successfully. productId={}, totalVariants={}", saved.getId(), saved.getVariants().size());

        return productAndCategoryMapper.mapToProductResponse(saved);
    }

    private List<ProductVariant> createProductVariants(@NotEmpty(message = "Provide at least one product variant") List<ProductVariantRequest> variantRequests, Product product) {
        List<ProductVariant> variants = new ArrayList<>();

        for(ProductVariantRequest request: variantRequests){
            if(productVariantRepository.existsBySku(request.getSku())){
                throw new IllegalArgsException("SKU already exists : " + request.getSku());
            }

            validatePrimaryImageAndImageOrder(request);

            ProductVariant variant = ProductVariant.builder()
                    .product(product)
                    .sku(request.getSku())
                    .listedPrice(request.getListedPrice())
                    .sellingPrice(request.getSellingPrice())
                    .weight(request.getWeight())
                    .length(request.getLength())
                    .width(request.getWidth())
                    .height(request.getHeight())
                    .attributes(request.getAttributes())
                    .availableStock(request.getAvailableStock())
                    .reservedStock(request.getReservedStock())
                    .build();

            List<ProductImage> images = createProductImages(
                    request.getProductImages(),
                    variant
            );

            variant.setProductImages(images);

            variants.add(variant);

            log.debug("Prepared variant sku={}", variant.getSku());
        }
        return variants;
    }

    private List<ProductImage> createProductImages(
            List<ProductImageRequest> requests,
            ProductVariant variant
    ) {

        return requests.stream()

                .sorted(
                        Comparator.comparing(
                                ProductImageRequest::getDisplayOrder
                        )
                )

                .map(image -> ProductImage.builder()

                        .productVariant(variant)

                        .imageUrl(image.getImageUrl())

                        .displayOrder(image.getDisplayOrder())

                        .primaryImage(image.getPrimaryImage())

                        .build())

                .collect(Collectors.toList());
    }

    private void validatePrimaryImageAndImageOrder(ProductVariantRequest request) {
        int count = 0;
        Set<Integer> orders = new HashSet<>();

        for(ProductImageRequest image : request.getProductImages()){

            if(!orders.add(image.getDisplayOrder())){
                throw new IllegalArgsException(
                        "Duplicate display order. Provide distinct image order"
                );
            }

            if(Boolean.TRUE.equals(image.getPrimaryImage())){
                count++;
            }
        }

        if(count > 1){

            throw new IllegalArgsException(
                    "Exactly one primary image is required for variant "
                            + request.getSku()
            );
        } else if(count == 0){
            throw new IllegalArgsException(
                    "Select one primary image for the variant: " + request.getSku()
            );
        }
    }

    private User getCurrentSeller() {

        String email = securityUtil.getCurrentUserEmail();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("Seller not found. email={}", email);
                    throw new ResourceNotFoundException("Seller not found");
                });
    }

    private void validateProductRequest(ProductRequest request){

        if(productRepository.existsBySlug(request.getSlug())){
            throw new ConflictException("Slug already exists");
        }

        if(request.getVariants().isEmpty()){
            throw new IllegalArgsException(
                    "At least one variant is required"
            );
        }

        Set<String> skus = new HashSet<>();

        for(ProductVariantRequest variant : request.getVariants()){

            if(!skus.add(variant.getSku())){
                throw new IllegalArgsException(
                        "Duplicate SKU found : "
                                + variant.getSku()
                );
            }
        }
    }

    @Cacheable(
            value = "products",
            key = "#id"
    )
    public ProductResponse getProductById(UUID id) {
        log.debug("Request received to fetch product by id={}", id);

        Product product = productRepository
                .findById(id)
                .orElseThrow(
                        () -> {
                            log.error("Product not found with id={}", id);
                            throw new ResourceNotFoundException("Product not found");
                        }
                );

        log.info("Product retrieved successfully - productId={}, productName={}, brand={}",
                product.getId(), product.getName(), product.getBrand());

        return productAndCategoryMapper.mapToProductResponse(product);
    }

    public List<ProductResponse> getProductsByCategory(UUID categoryId) {
        log.debug("Request received to fetch products by category={}", categoryId);

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + categoryId));

        List<ProductResponse> products = productRepository.findByCategoryId(categoryId)
                .stream()
                .map(productAndCategoryMapper::mapToProductResponse)
                .collect(Collectors.toList());

        log.info("Found {} products in category: {}", products.size(), categoryId);

        if (products.isEmpty()) {
            log.warn("No products found in category: {}", categoryId);
        }

        return products;
    }

    public Page<ProductResponse> getProducts(int page, int size, String sortBy) {
        log.debug("Request received to fetch products with pagination - page={}, size={}, sortBy={}",
                page, size, sortBy);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, sortBy));
        log.debug("Created pageable object: {}", pageable);

        Page<ProductResponse> productPage = productRepository.findAll(pageable)
                .map(productAndCategoryMapper::mapToProductResponse);

        log.info("Retrieved page {} of products. Total elements: {}, Total pages: {}",
                page, productPage.getTotalElements(), productPage.getTotalPages());

        return productPage;
    }

    public Page<ProductSearchResponse> search(ProductSearchRequest request) {
        log.debug("Request received to search products with filters");
        log.debug("Search request filters: {}", request);

        Specification<Product> spec = Specification.allOf(
                ProductSpecification.keywordContains(request.getKeyword()),
                ProductSpecification.hasBrand(request.getBrand()),
                ProductSpecification.hasCategory(request.getCategoryId()),
                ProductSpecification.priceBetween(request.getMinPrice(), request.getMaxPrice())
        );

        log.info(
                "Product search initiated. keyword={}, brand={}, categoryId={}, page={}, size={}",
                request.getKeyword(),
                request.getBrand(),
                request.getCategoryId(),
                request.getPage(),
                request.getSize()
        );

        // Handle sorting - special case for price
        Sort sort = buildSortForSearch(request);
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), sort);
        log.debug("Created pageable object for search: {}", pageable);

        Page<Product> productPage;

        // If sorting by price, use custom repository methods
        if (isPriceSort(request.getSortBy())) {
            // First get all products matching specification
            List<Product> products = productRepository.findAll(spec);
            log.debug("Found {} products matching criteria before price sorting", products.size());

            if (products.isEmpty()) {
                productPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
            } else {
                // Use repository methods for efficient sorting
                List<Product> sortedProducts;
                if ("DESC".equalsIgnoreCase(request.getSortDirection())) {
                    sortedProducts = productRepository.findProductsSortedByMinPriceDesc(products);
                } else {
                    sortedProducts = productRepository.findProductsSortedByMinPriceAsc(products);
                }
                log.debug("Sorted {} products by price", sortedProducts.size());

                // Apply pagination manually
                int start = (int) pageable.getOffset();
                int end = Math.min((start + pageable.getPageSize()), sortedProducts.size());

                if (start < sortedProducts.size()) {
                    List<Product> pagedProducts = sortedProducts.subList(start, end);
                    productPage = new PageImpl<>(
                            pagedProducts,
                            pageable,
                            sortedProducts.size()
                    );
                } else {
                    productPage = new PageImpl<>(Collections.emptyList(), pageable, sortedProducts.size());
                }
            }
        } else {
            // Normal flow with Specification and pagination
            productPage = productRepository.findAll(spec, pageable);
        }

        Page<ProductSearchResponse> products = productPage.map(productAndCategoryMapper::mapToProductSearchResponse);

        log.info("Search completed. Found {} products matching criteria. Page: {}/{}",
                products.getTotalElements(), request.getPage(), products.getTotalPages());

        if (products.isEmpty()) {
            log.warn("No products found matching the search criteria");
        }

        return products;
    }

    private boolean isPriceSort(String sortBy) {
        if (sortBy == null) return false;
        String lower = sortBy.toLowerCase();
        return lower.equals("price") ||
                lower.equals("sellingprice") ||
                lower.equals("listedprice") ||
                lower.equals("minprice") ||
                lower.equals("maxprice") ||
                lower.equals("startingprice");
    }

    private Sort buildSortForSearch(ProductSearchRequest request) {
        String sortBy = request.getSortBy();
        String direction = request.getSortDirection();

        // If sorting by price, return unsorted (we'll handle it via repository)
        if (isPriceSort(sortBy)) {
            return Sort.unsorted();
        }

        // Default sort
        if (sortBy == null || sortBy.trim().isEmpty()) {
            return Sort.by("name").ascending();
        }

        Sort.Direction dir = Sort.Direction.ASC;
        if ("DESC".equalsIgnoreCase(direction)) {
            dir = Sort.Direction.DESC;
        }

        // Map sort field to entity field
        String actualField = mapSortField(sortBy);
        return Sort.by(dir, actualField);
    }

    private String mapSortField(String sortBy) {
        if (sortBy == null) return "name";

        switch (sortBy.toLowerCase()) {
            case "productid":
                return "id";
            case "productname":
                return "name";
            case "brandname":
            case "brand":
                return "brand";
            case "category":
                return "category.name";
            case "createddate":
            case "createdat":
                return "createdAt";
            case "updateddate":
            case "updatedat":
                return "updatedAt";
            case "reviews":
            case "reviewcount":
                return "reviewCount";
            case "rating":
            case "averagerating":
                return "averageRating";
            default:
                return sortBy;
        }
    }

    @CacheEvict(
            value = "products",
            key = "#id"
    )
    @Transactional
    public void deleteProduct(UUID id) {
        log.debug("Request received to delete product with id={}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(
                        () -> {
                            log.error("Product not found with id={} for deletion", id);
                            throw new ResourceNotFoundException("Product not found");
                        }
                );

        log.info("Product found for deletion - productId={}, productName={}, sellerId={}",
                product.getId(), product.getName(), product.getSeller().getId());
        securityUtil.validateProductOwnership(product.getSeller().getEmail());
        productRepository.delete(product);

        log.info("Product deleted successfully - productId={}, productName={}", id, product.getName());
    }

    @CacheEvict(
            value = "products",
            key = "#id"
    )
    @Transactional
    public ProductResponse updateProduct(UUID id, ProductUpdateRequest request) {
        log.debug("Request received to update product with id={}", id);
        log.debug("Product update request details: {}", request);

        Product product = productRepository.findById(id).orElseThrow(
                () -> {
                    log.error("Product not found with id={} for update", id);
                    throw new ResourceNotFoundException("Product not found");
                }
        );

        log.debug("Current product details before update - name={}, brand={}, category={}",
                product.getName(), product.getBrand(), product.getCategory().getName());

        log.debug("Validating ownership for product update - sellerEmail={}", product.getSeller().getEmail());
        securityUtil.validateProductOwnership(product.getSeller().getEmail());
        log.debug("Product ownership validation successful");

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        String oldName = product.getName();
        String oldBrand = product.getBrand();

        product.setName(request.getName());
        product.setSlug(request.getSlug());
        product.setBrand(request.getBrand());
        product.setShortDescription(request.getShortDescription());
        product.setLongDescription(request.getLongDescription());
        product.setCategory(category);

        log.debug("Product updated with new values - name={}, brand={}, category={}",
                request.getName(), request.getBrand(), category.getName());

        Product saved = productRepository.save(product);

        log.info("Product updated successfully - productId={}", id);
        log.info("Changes applied - Name: '{}' -> '{}', Brand: '{}' -> '{}'",
                oldName, saved.getName(), oldBrand, saved.getBrand());

        return productAndCategoryMapper.mapToProductResponse(saved);
    }

    @Transactional
    public ProductImageResponse uploadProductImage(UUID productId, MultipartFile file) {
        Product product = productRepository.findById(productId).orElseThrow(
                () -> new ResourceNotFoundException("Product not found")
        );
        securityUtil.validateProductOwnership(product.getSeller().getEmail());

        String imageUrl = fileUploadService.uploadProductImage(file);

        ProductImage productImage = ProductImage.builder()
                .fileName(file.getOriginalFilename())
                .fileSize(file.getSize())
                .fileType(file.getContentType())
                .imageUrl(imageUrl)
                .build();

        ProductImage saved = productImageRepository.save(productImage);
        log.info("Product image uploaded successfully for productId={}", productId);
        log.info("Uploaded image id = {}", saved.getId());

        return ProductImageResponse.builder()
                .imageId(saved.getId())
                .fileName(saved.getFileName())
                .imageUrl(saved.getImageUrl())
                .build();
    }
}