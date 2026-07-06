package com.example.orderinventory.product.controller;

import com.example.orderinventory.common.exception.BusinessException;
import com.example.orderinventory.common.exception.GlobalExceptionHandler;
import com.example.orderinventory.common.result.ErrorCode;
import com.example.orderinventory.product.dto.ProductStatusUpdateRequest;
import com.example.orderinventory.product.entity.Product;
import com.example.orderinventory.product.service.ProductService;
import com.example.orderinventory.product.vo.ProductStatusUpdateVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProductControllerStatusTest {

    private static final String STATUS_URL = "/api/v1/products/{productId}/status";

    private ProductService productService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        productService = mock(ProductService.class);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new ProductController(productService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void changeProductStatusShouldReturnUpdatedIdAndStatus() throws Exception {
        Product product = new Product();
        product.setId(1001L);
        product.setProductStatus(0);

        when(productService.updateProductStatus(eq(1001L), any(ProductStatusUpdateRequest.class)))
                .thenReturn(ProductStatusUpdateVO.of(1001L,0));

        mockMvc.perform(patch(STATUS_URL, 1001L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productStatus": 0
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1001))
                .andExpect(jsonPath("$.data.productStatus").value(0));

        ArgumentCaptor<ProductStatusUpdateRequest> requestCaptor =
                ArgumentCaptor.forClass(ProductStatusUpdateRequest.class);
        verify(productService).updateProductStatus(eq(1001L), requestCaptor.capture());
        assertThat(requestCaptor.getValue().getProductStatus()).isEqualTo(0);
    }

    @Test
    void changeProductStatusShouldRejectStatusGreaterThanOne() throws Exception {
        mockMvc.perform(patch(STATUS_URL, 1001L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productStatus": 2
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PARAM_ERROR"))
                .andExpect(jsonPath("$.success").value(false));

        verify(productService, never()).updateProductStatus(any(), any());
    }

    @Test
    void changeProductStatusShouldRejectMissingStatus() throws Exception {
        mockMvc.perform(patch(STATUS_URL, 1001L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PARAM_ERROR"))
                .andExpect(jsonPath("$.success").value(false));

        verify(productService, never()).updateProductStatus(any(), any());
    }

    @Test
    void changeProductStatusShouldRejectEmptyBody() throws Exception {
        mockMvc.perform(patch(STATUS_URL, 1001L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PARAM_ERROR"))
                .andExpect(jsonPath("$.success").value(false));

        verify(productService, never()).updateProductStatus(any(), any());
    }

    @Test
    void changeProductStatusShouldRejectMalformedJson() throws Exception {
        mockMvc.perform(patch(STATUS_URL, 1001L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productStatus\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PARAM_ERROR"))
                .andExpect(jsonPath("$.success").value(false));

        verify(productService, never()).updateProductStatus(any(), any());
    }

    @Test
    void changeProductStatusShouldReturnNotFoundWhenProductDoesNotExist() throws Exception {
        when(productService.updateProductStatus(eq(9999L), any(ProductStatusUpdateRequest.class)))
                .thenThrow(new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "product not found"));

        mockMvc.perform(patch(STATUS_URL, 9999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productStatus": 1
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"))
                .andExpect(jsonPath("$.success").value(false));
    }
}
