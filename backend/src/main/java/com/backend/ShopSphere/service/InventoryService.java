package com.backend.ShopSphere.service;

import com.backend.ShopSphere.entity.*;
import com.backend.ShopSphere.exception.IllegalArgsException;
import com.backend.ShopSphere.exception.ResourceNotFoundException;
import com.backend.ShopSphere.repository.OrderItemRepository;
import com.backend.ShopSphere.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {
    private final ProductVariantRepository productVariantRepository;
    private final OrderItemRepository orderItemRepository;

    @Transactional
    public void reserveStock(Cart cart){
        log.info("Reserving inventory for cartId={}", cart.getId());

        for(CartItem item: cart.getCartItems()){
            ProductVariant productVariant = item.getProductVariant();

            if(item.getQuantity() > productVariant.getAvailableStock()){
                throw new IllegalArgsException("Insufficient stock for "+ productVariant.getSku());
            }

            productVariant.setAvailableStock(productVariant.getAvailableStock() - item.getQuantity());
            productVariant.setReservedStock(productVariant.getReservedStock() + item.getQuantity());

            productVariantRepository.save(productVariant);
            log.info("Reserved inventory. sku={}, available={}, reserved={}",
                    productVariant.getSku(),
                    productVariant.getAvailableStock(),
                    productVariant.getReservedStock());
        }
    }

    @Transactional
    public void reserveStock(ProductVariant productVariant, Integer quantity){
        if(quantity > productVariant.getAvailableStock()){
            throw new IllegalArgsException("Insufficient stock for "+ productVariant.getSku());
        }

        productVariant.setAvailableStock(productVariant.getAvailableStock() - quantity);
        productVariant.setReservedStock(productVariant.getReservedStock() + quantity);

        productVariantRepository.save(productVariant);
        log.info("Reserved inventory. sku={}, available={}, reserved={}",
                productVariant.getSku(),
                productVariant.getAvailableStock(),
                productVariant.getReservedStock());
    }

    @Transactional
    public void confirmStock(Order order){

        log.info("Confirming inventory for orderId={}", order.getId());
        List<OrderItem> orderItemList = orderItemRepository.findByOrderId(order.getId());

        for(OrderItem item : orderItemList){
            ProductVariant variant = productVariantRepository
                            .findById(item.getProductVariantId())
                            .orElseThrow(
                                    () -> new ResourceNotFoundException("Variant not found")
                            );

            variant.setReservedStock(variant.getReservedStock() - item.getQuantity());
            productVariantRepository.save(variant);
            log.info("Inventory confirmed. sku={}, reserved={}", variant.getSku(), variant.getReservedStock());
        }
    }

    @Transactional
    public void releaseStock(Order order){
        log.info("Releasing stock for orderId: {}", order.getId());
        List<OrderItem> orderItemList = orderItemRepository.findByOrderId(order.getId());

        for(OrderItem item: orderItemList){
            ProductVariant variant = productVariantRepository.findById(item.getProductVariantId()).orElseThrow(
                    () -> new ResourceNotFoundException("Variant not found")
            );

            variant.setReservedStock(variant.getReservedStock() - item.getQuantity());
            variant.setAvailableStock(variant.getAvailableStock() + item.getQuantity());

            productVariantRepository.save(variant);
            log.info("Inventory released for {}. Available stock: {}. Reserved stock: {}", variant.getSku(), variant.getAvailableStock(), variant.getReservedStock());
        }
    }

    @Transactional
    public void releaseStock(OrderItem orderItem) {
        ProductVariant variant = productVariantRepository
                .findById(orderItem.getProductVariantId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Product variant not found"));

        variant.setReservedStock(variant.getReservedStock() - orderItem.getQuantity());
        variant.setAvailableStock(variant.getAvailableStock() + orderItem.getQuantity());

        productVariantRepository.save(variant);

        log.info(
                "Inventory reservation released. orderItemId={}, variantId={}, quantity={}",
                orderItem.getId(),
                variant.getId(),
                orderItem.getQuantity()
        );
    }

    @Transactional
    public void restoreStock(OrderItem orderItem) {
        ProductVariant variant = productVariantRepository
                .findById(orderItem.getProductVariantId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Product variant not found"));

        variant.setAvailableStock(variant.getAvailableStock() + orderItem.getQuantity());

        productVariantRepository.save(variant);

        log.info(
                "Inventory restored. orderItemId={}, variantId={}, quantity={}",
                orderItem.getId(),
                variant.getId(),
                orderItem.getQuantity()
        );
    }
}
