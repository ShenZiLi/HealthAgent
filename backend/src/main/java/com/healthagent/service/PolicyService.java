package com.healthagent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.healthagent.dto.PolicyQueryRequest;
import com.healthagent.dto.PolicyInfo;
import com.healthagent.entity.PolInfoEntity;
import com.healthagent.mapper.PolInfoMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 保单查询服务
 */
@Slf4j
@Service
public class PolicyService {

    @Autowired
    private PolInfoMapper polInfoMapper;

    /**
     * 根据保单号查询
     *
     * @param polNo 保单号
     * @return 保单信息
     */
    public Optional<PolInfoEntity> getByPolNo(String polNo) {
        log.info("查询保单，保单号: {}", polNo);
        LambdaQueryWrapper<PolInfoEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PolInfoEntity::getPolNo, polNo);
        List<PolInfoEntity> results = polInfoMapper.selectList(wrapper);
        return results.stream().findFirst();
    }

    /**
     * 根据投保人姓名查询
     *
     * @param policyHolderName 投保人姓名
     * @return 保单列表
     */
    public List<PolInfoEntity> getByPolicyHolderName(String policyHolderName) {
        log.info("根据投保人姓名查询保单: {}", policyHolderName);
        LambdaQueryWrapper<PolInfoEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(PolInfoEntity::getPolicyHolderName, policyHolderName)
                .orderByDesc(PolInfoEntity::getCreateTime);
        return polInfoMapper.selectList(wrapper);
    }

    /**
     * 根据身份证号查询
     *
     * @param idCardNo 身份证号
     * @return 保单列表
     */
    public List<PolInfoEntity> getByIdCardNo(String idCardNo) {
        log.info("根据身份证号查询保单: {}", idCardNo);
        LambdaQueryWrapper<PolInfoEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PolInfoEntity::getIdCardNo, idCardNo)
                .orderByDesc(PolInfoEntity::getCreateTime);
        return polInfoMapper.selectList(wrapper);
    }

    /**
     * 条件查询保单
     *
     * @param request 查询条件
     * @return 保单列表
     */
    public List<PolInfoEntity> queryPolicies(PolicyQueryRequest request) {
        log.info("条件查询保单: {}", request);
        LambdaQueryWrapper<PolInfoEntity> wrapper = buildQueryWrapper(request);
        return polInfoMapper.selectList(wrapper);
    }

    /**
     * 分页查询保单
     *
     * @param request  查询条件
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    public Page<PolInfoEntity> queryPoliciesPage(PolicyQueryRequest request, long pageNum, long pageSize) {
        log.info("分页查询保单，页码: {}, 每页: {}", pageNum, pageSize);
        LambdaQueryWrapper<PolInfoEntity> wrapper = buildQueryWrapper(request);
        Page<PolInfoEntity> page = new Page<>(pageNum, pageSize);
        return polInfoMapper.selectPage(page, wrapper);
    }

    /**
     * 查询所有保单
     *
     * @return 保单列表
     */
    public List<PolInfoEntity> getAllPolicies() {
        log.info("查询所有保单");
        return polInfoMapper.selectList(null);
    }

    /**
     * 删除保单（逻辑删除）
     *
     * @param polNo 保单号
     * @return 是否成功
     */
    public boolean deleteByPolNo(String polNo) {
        log.info("删除保单: {}", polNo);
        PolInfoEntity entity = getByPolNo(polNo).orElse(null);
        if (entity == null) {
            return false;
        }
        return polInfoMapper.deleteById(entity.getId()) > 0;
    }

    /**
     * 更新保单信息
     *
     * @param entity 保单信息
     * @return 是否成功
     */
    public boolean updatePolicy(PolInfoEntity entity) {
        log.info("更新保单: {}", entity.getPolNo());
        return polInfoMapper.updateById(entity) > 0;
    }

    /**
     * 新增保单
     *
     * @param entity 保单信息
     * @return 是否成功
     */
    public boolean createPolicy(PolInfoEntity entity) {
        log.info("新增保单: {}", entity.getPolNo());
        return polInfoMapper.insert(entity) > 0;
    }

    /**
     * 构建查询条件
     */
    private LambdaQueryWrapper<PolInfoEntity> buildQueryWrapper(PolicyQueryRequest request) {
        LambdaQueryWrapper<PolInfoEntity> wrapper = new LambdaQueryWrapper<>();

        if (request.getPolNo() != null && !request.getPolNo().isEmpty()) {
            wrapper.eq(PolInfoEntity::getPolNo, request.getPolNo());
        }
        if (request.getPolicyHolderName() != null && !request.getPolicyHolderName().isEmpty()) {
            wrapper.like(PolInfoEntity::getPolicyHolderName, request.getPolicyHolderName());
        }
        if (request.getIdCardNo() != null && !request.getIdCardNo().isEmpty()) {
            wrapper.eq(PolInfoEntity::getIdCardNo, request.getIdCardNo());
        }
        if (request.getStatus() != null && !request.getStatus().isEmpty()) {
            wrapper.eq(PolInfoEntity::getStatus, request.getStatus());
        }

        wrapper.orderByDesc(PolInfoEntity::getCreateTime);
        return wrapper;
    }
}
