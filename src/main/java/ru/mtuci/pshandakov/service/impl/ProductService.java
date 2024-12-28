package ru.mtuci.pshandakov.service.impl;

import lombok.RequiredArgsConstructor;
import ru.mtuci.pshandakov.model.Product;
import ru.mtuci.pshandakov.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    public Product saveProduct(Product product) {
        return productRepository.save(product);
    }


    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }


    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }


    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }
}
