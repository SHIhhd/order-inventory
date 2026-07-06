package com.example.orderinventory.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.orderinventory.common.exception.BusinessException;
import com.example.orderinventory.common.result.ErrorCode;
import com.example.orderinventory.common.result.PageResult;
import com.example.orderinventory.product.dto.ProductCreateRequest;
import com.example.orderinventory.product.dto.ProductStatusUpdateRequest;
import com.example.orderinventory.product.entity.Product;
import com.example.orderinventory.product.mapper.ProductMapper;
import com.example.orderinventory.product.service.ProductService;
import com.example.orderinventory.product.vo.ProductStatusUpdateVO;
import com.example.orderinventory.product.vo.ProductVO;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

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
 * @date 2026/7/1 14:22
 * @description 类的详细说明
 */
@Service
public class ProductServiceImpl  extends ServiceImpl<ProductMapper, Product> implements ProductService {

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ProductVO createProduct(ProductCreateRequest productCreateRequest) {
        //如果其他类调用需要业务边界校验
        if (productCreateRequest == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "商品创建参数不能为空");
        }
        Product product = buildProduct(productCreateRequest);
        try {
             baseMapper.insert(product);
        }catch (DuplicateKeyException e){
            throw new BusinessException(ErrorCode.PRODUCT_SKU_DUPLICATE);
        }
        ProductVO productVO =  ProductVO.from(product);
        return productVO;
    }

    /**
     * ①
     *@Transactional(readOnly = true)。
     * 这能向 Spring 事务管理器和部分数据库驱动表达只读意图，减少误写风险。
     *②
     * 如果是分页查询，使用selectPage语义更明确
     * Page<Product> productPage = baseMapper.selectPage(page, queryWrapper);
     * productPage.getRecords()
     * productPage.getTotal()
     *
     *
     * @param pageNo
     * @param pageSize
     * @param keyword
     * @param productStatus
     * @return
     */
    @Override
    @Transactional(readOnly = true)
    public PageResult<ProductVO> getProductPage(Integer pageNo, Integer pageSize,
                                                  String keyword, Integer productStatus) {
        if(pageNo == null || pageNo < 1){
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "pageNo must be greater than or equal to 1");
        }
        if(pageSize == null || pageSize < 1 || pageSize > 100){
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "pageSize must be greater than or equal to 1 and" +
                    "less than or equal to 100");
        }
        if (productStatus != null && productStatus != 0 && productStatus != 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "productStatus must be 0 or 1");
        }

        boolean hasKeyword = keyword != null && !keyword.isBlank();
        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();
        //TODO like 默认通常是 %keyword%。索引会失效，后续重新优化
        queryWrapper.and(hasKeyword,keywordWrapper -> keywordWrapper
                        .like(Product::getProductName , keyword)
                        .or()
                        .like(Product::getSkuCode , keyword))
                .eq(productStatus!=null , Product::getProductStatus , productStatus)
                .orderByDesc(Product::getCreateTime);
        Page<Product> productPage = new Page<>(pageNo, pageSize);
        productPage = baseMapper.selectPage(productPage, queryWrapper);
        List<ProductVO> productVoList = productPage.getRecords().stream()
                .map(ProductVO::from)
                .collect(Collectors.toList());
        return PageResult.of(productVoList,pageNo,pageSize,productPage.getTotal());
    }

    @Override
    @Transactional
    public ProductStatusUpdateVO updateProductStatus(Long productId,
                                         ProductStatusUpdateRequest productStatusUpdateRequest) {
        if(productStatusUpdateRequest == null){
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "requestBody must not be null");
        }
        Integer productStatus = productStatusUpdateRequest.getProductStatus();
        if (productStatus == null
                ||( productStatus != 0 && productStatus != 1 )) {
            throw new BusinessException(ErrorCode.PRODUCT_STATUS_INVALID,
                    "productStatus must be 0 or 1");
        }
        if (productId == null || productId <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "productId must be greater than 0");
        }
        Product product = baseMapper.selectById(productId);
        if(product == null ){
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND,
                    "商品不存在");
        }
        product.setProductStatus(productStatus);
        int i = baseMapper.updateById(product);
        /**
         * 【学习】
         * 这里使用了乐观锁，所以没有查到有两种可能 ①乐观锁版本更新 ②商品不存在/已删除
         * 为什么还要由第二次判断latestProduct == null?
         * 因为有可能在上面第一次 baseMapper.selectById(productId)，到 int i = baseMapper.updateById(product); 有线程并发删除了该条数据
         */
        if (i != 1) {
            Product latestProduct = baseMapper.selectById(productId);
            if(latestProduct == null){
                throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND,"商品不存在");
            }
            throw new BusinessException(ErrorCode.CONCURRENT_UPDATE_FAILED, "并发更新失败");
        }

        /**
         * 【学习】
         * 更新前：return ProductStatusUpdateVO.from(product);
         * ProductStatusUpdateVO.from(product) 容易让人误以为这是“数据库更新后的商品对象”。
         * from 通常表示“从某个对象转换而来”，比如 from(Product product)。现在是两个基础字段，
         * 更推荐： of
         */
        return ProductStatusUpdateVO.of(productId,productStatus);
    }

    private Product buildProduct(ProductCreateRequest request) {
        Product product = new Product();
        product.setSkuCode(request.getSkuCode());
        product.setProductName(request.getProductName());
        product.setProductStatus(request.getProductStatus());
        product.setSalePrice(request.getSalePrice());
        product.setRemark(request.getRemark());
        return product;
    }
}
