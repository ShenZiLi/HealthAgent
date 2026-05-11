package com.healthagent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.healthagent.dto.PolicyInfo;
import com.healthagent.dto.PolicyQueryRequest;
import com.healthagent.entity.PolInfoEntity;
import com.healthagent.mapper.PolInfoMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

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
     * @param request 查询条件
     * @param pageNum 页码
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

    // ==================== 以下方法为兼容旧接口保留，供 SmartChatService 使用 ====================

    private static final Map<String, List<PolicyInfo>> MOCK_POLICIES = new HashMap<>();

    static {
        List<PolicyInfo> user1Policies = new ArrayList<>();
        user1Policies.add(new PolicyInfo(
            "POL20240001",
            "health",
            "健康医疗保险",
            "active",
            new BigDecimal("3650.00"),
            new BigDecimal("500000.00"),
            new Date(System.currentTimeMillis() - 86400000L * 180),
            new Date(System.currentTimeMillis() + 86400000L * 185),
            "平安保险"
        ));
        user1Policies.add(new PolicyInfo(
            "POL20240002",
            "life",
            "终身寿险",
            "active",
            new BigDecimal("12000.00"),
            new BigDecimal("1000000.00"),
            new Date(System.currentTimeMillis() - 86400000L * 365),
            null,
            "中国人寿"
        ));
        user1Policies.add(new PolicyInfo(
            "POL20230015",
            "accident",
            "意外伤害保险",
            "expired",
            new BigDecimal("280.00"),
            new BigDecimal("100000.00"),
            new Date(System.currentTimeMillis() - 86400000L * 400),
            new Date(System.currentTimeMillis() - 86400000L * 35),
            "太平洋保险"
        ));

        List<PolicyInfo> user2Policies = new ArrayList<>();
        user2Policies.add(new PolicyInfo(
            "POL20240015",
            "health",
            "重大疾病保险",
            "active",
            new BigDecimal("5800.00"),
            new BigDecimal("800000.00"),
            new Date(System.currentTimeMillis() - 86400000L * 90),
            new Date(System.currentTimeMillis() + 86400000L * 275),
            "友邦保险"
        ));

        List<PolicyInfo> user3Policies = new ArrayList<>();
        user3Policies.add(new PolicyInfo(
            "POL20240020",
            "property",
            "家庭财产保险",
            "active",
            new BigDecimal("520.00"),
            new BigDecimal("500000.00"),
            new Date(System.currentTimeMillis() - 86400000L * 60),
            new Date(System.currentTimeMillis() + 86400000L * 305),
            "中华保险"
        ));

        MOCK_POLICIES.put("user001", user1Policies);
        MOCK_POLICIES.put("user002", user2Policies);
        MOCK_POLICIES.put("user003", user3Policies);
    }

    /**
     * 查询用户保单（兼容旧接口 - 使用Mock数据）
     */
    public List<PolicyInfo> getUserPolicies(String userId) {
        log.info("查询用户 {} 的保单信息", userId);
        
        List<PolicyInfo> policies = MOCK_POLICIES.get(userId);
        if (policies == null) {
            return new ArrayList<>();
        }
        
        return policies.stream()
            .filter(policy -> "active".equals(policy.getStatus()))
            .sorted(Comparator.comparing(PolicyInfo::getStartDate).reversed())
            .toList();
    }

    /**
     * 查询用户所有保单（兼容旧接口 - 使用Mock数据）
     */
    public List<PolicyInfo> getAllUserPolicies(String userId) {
        log.info("查询用户 {} 的所有保单信息（包括已过期）", userId);
        
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        List<PolicyInfo> policies = MOCK_POLICIES.get(userId);
        if (policies == null) {
            return new ArrayList<>();
        }
        
        return policies.stream()
            .sorted(Comparator.comparing(PolicyInfo::getStartDate).reversed())
            .toList();
    }

    /**
     * 根据保单号查询（兼容旧接口）
     */
    public Optional<PolicyInfo> getPolicyById(String userId, String policyId) {
        List<PolicyInfo> policies = getUserPolicies(userId);
        return policies.stream()
            .filter(p -> p.getPolicyId().equals(policyId))
            .findFirst();
    }

    /**
     * 格式化保单信息为文本（兼容旧接口）
     */
    public String formatPoliciesAsText(List<PolicyInfo> policies) {
        if (policies == null || policies.isEmpty()) {
            return "未查询到有效保单";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("您共有 ").append(policies.size()).append(" 份有效保单：\n\n");

        for (int i = 0; i < policies.size(); i++) {
            PolicyInfo policy = policies.get(i);
            sb.append("【保单").append(i + 1).append("】\n");
            sb.append("• 保单号：").append(policy.getPolicyId()).append("\n");
            sb.append("• 产品名称：").append(policy.getPolicyName()).append("\n");
            sb.append("• 保险公司：").append(policy.getInsuranceCompany()).append("\n");
            sb.append("• 保障额度：¥").append(policy.getCoverage()).append("\n");
            sb.append("• 年缴保费：¥").append(policy.getPremium()).append("\n");
            sb.append("• 生效日期：").append(formatDate(policy.getStartDate())).append("\n");
            if (policy.getEndDate() != null) {
                sb.append("• 到期日期：").append(formatDate(policy.getEndDate())).append("\n");
            } else {
                sb.append("• 保障期限：终身\n");
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    private String formatDate(Date date) {
        if (date == null) {
            return "未知";
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(date);
    }
}
