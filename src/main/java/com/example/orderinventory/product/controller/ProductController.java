package com.example.orderinventory.product.controller;

import com.example.orderinventory.common.result.ApiResult;
import com.example.orderinventory.common.result.PageResult;
import com.example.orderinventory.product.dto.ProductCreateRequest;
import com.example.orderinventory.product.dto.ProductPageRequest;
import com.example.orderinventory.product.dto.ProductStatusUpdateRequest;
import com.example.orderinventory.product.service.ProductService;
import com.example.orderinventory.product.vo.ProductStatusUpdateVO;
import com.example.orderinventory.product.vo.ProductVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 请遵循六道之力
 * 【①】这个字段会不会是 null？
 * 【②】这个类需不需要 setter？
 * 【③】有没有统一创建方法？
 * 【④】有没有泛型？
 * 【⑤】有没有依赖具体实现类？
 * 【⑥】传错参数会不会悄悄生成错误结果？
 *
 * @author 我他妈莱纳
 * @date 2026/7/1 14:23
 * @description 类的详细说明
 */
@RestController
@Validated
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    /**
     * 新增商品基础信息
     * 新增成功后，系统只创建 `product` 记录，不自动创建库存记录。
     * 库存需要调用“初始化库存”接口单独处理。
     * @return
     */
    @PostMapping
    public ApiResult<ProductVO> createProduct(@Valid @RequestBody ProductCreateRequest
                                                        productCreateRequest){
        ProductVO productVO = productService.createProduct(productCreateRequest);
        return ApiResult.success(productVO);
    }

    @GetMapping
    public ApiResult<PageResult<ProductVO>> getProductPage(@Valid ProductPageRequest productPageRequest){
        PageResult<ProductVO> pageResult = productService.getProductPage(productPageRequest.getPageNo(),
                productPageRequest.getPageSize(),
                productPageRequest.getKeyword(),
                productPageRequest.getProductStatus());

        return ApiResult.success(pageResult);
    }

    /**
     * 【学习】
     * 一 、这个接口是个很好的学习思路
     * PATCH /api/v1/products/{productId}/status
     * Content-Type: application/json
     *
     * {
     *   "productStatus": 0
     * }
     * 含义很清楚：
     * /products/{productId}/status：我要改哪个商品的状态
     * body 里的 productStatus：我要把状态改成什么
     *
     * 如果所有的信息都是用路径传参数：PATCH /api/v1/products/1/status/0
     * 缺点：
     * 路径越来越像“动作参数集合”，语义不够稳定
     * 以后如果还要传 reason、operatorId、remark，路径会很难看
     * 路径更适合定位资源，不适合承载复杂修改内容
     * body 更适合放业务数据，也更方便做 JSON 校验和扩展
     *
     * 如果全部由json传参
     * PATCH /api/v1/products/status
     *
     * {
     *   "productId": 1,
     *   "productStatus": 0
     * }
     * 缺点
     * URL 看不出操作的是哪个资源
     * 不如 /products/1/status 直观
     * 不太符合 REST 风格里“资源由 URL 标识”的习惯
     * 日志、网关、权限控制、接口文档里，路径参数更容易表达资源范围
     *
     * 所以推荐：
     * 路径参数：资源身份，比如 productId、orderId、userId
     * JSON body：要创建/修改的数据，比如 productStatus、quantity、remark
     * Query 参数：查询筛选条件，比如 pageNo、pageSize、keyword、status
     * 简单判断规则：
     * 这个值是用来找到1谁？ 放路径：/products/{productId}
     * 这个值是要改成什么？ 放 JSON：{"productStatus": 0}
     * 这个值是查询/过滤/分页？ 放 query：?pageNo=1&pageSize=10&status=
     *
     * @param productId
     * @param productStatusUpdateRequest
     * @return
     */
    @PatchMapping("/{productId}/status")
    public ApiResult<ProductStatusUpdateVO> updateProductStatus(@Positive(message = "productId must be greater than 0") @PathVariable Long productId,
                                                    @Valid @RequestBody ProductStatusUpdateRequest productStatusUpdateRequest){
        ProductStatusUpdateVO productStatusUpdateVO =  productService.updateProductStatus(productId,productStatusUpdateRequest);
        return ApiResult.success(productStatusUpdateVO);
    }

}
