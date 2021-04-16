package es.urjc.code.daw.marketplace.api.product.controller;

import es.urjc.code.daw.marketplace.api.product.dto.FindProductResponseDto;
import es.urjc.code.daw.marketplace.api.product.mapper.RestProductMapper;
import es.urjc.code.daw.marketplace.domain.Product;
import es.urjc.code.daw.marketplace.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
public class ProductRestController {

    private static final String BASE_ROUTE = "/api/products";

    private final ProductService productService;
    private final RestProductMapper restProductMapper;

    public ProductRestController(ProductService productService, RestProductMapper restProductMapper) {
        this.productService = productService;
        this.restProductMapper = restProductMapper;
    }

    @Operation(summary = "Finds a list of all products")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "The list of products was found successfully",
                    content = {@Content(
                            array = @ArraySchema(schema = @Schema(implementation = FindProductResponseDto.class))
                    )}
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Can not access to find products",
                    content = @Content
            ),

            @ApiResponse(
                    responseCode = "404",
                    description = "There were no more products to be shown",
                    content = @Content
            ),

    })
    @RequestMapping(
            path = BASE_ROUTE,
            method = RequestMethod.GET
    )
    public ResponseEntity<List<FindProductResponseDto>> findAllProducts() {
        // Find all the available products
        List<Product> products = productService.findAllProducts();
        // If there are no products return the appropriate response
        if(products.isEmpty()) return ResponseEntity.notFound().build();
        // All the found products map them to DTO's
        List<FindProductResponseDto> response = products.stream().map(restProductMapper::asFindResponse).collect(Collectors.toList());
        // Send the successful response with all the DTO products
        return ResponseEntity.ok(response);
    }

}
