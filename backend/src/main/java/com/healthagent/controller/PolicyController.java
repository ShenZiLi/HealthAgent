package com.healthagent.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.healthagent.common.Result;
import com.healthagent.dto.PolicyQueryRequest;
import com.healthagent.entity.PolInfoEntity;
import com.healthagent.service.PolicyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 保单查询控制器
 */
@Slf4j
@Tag(name = "保单查询接口")
@RestController
@RequestMapping("/api/policy")
public class PolicyController {

    @Autowired
    private PolicyService policyService;

    @Operation(summary = "条件查询保单")
    @PostMapping("/query")
    public Result<List<PolInfoEntity>> queryPolicies(@RequestBody PolicyQueryRequest request) {
        try {
            List<PolInfoEntity> policies = policyService.queryPolicies(request);
            return Result.success(policies);
        } catch (Exception e) {
            log.error("查询保单失败", e);
            return Result.error("查询保单失败: " + e.getMessage());
        }
    }

    @Operation(summary = "分页查询保单")
    @PostMapping("/page")
    public Result<Page<PolInfoEntity>> queryPoliciesPage(
            @RequestBody PolicyQueryRequest request,
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize) {
        try {
            Page<PolInfoEntity> page = policyService.queryPoliciesPage(request, pageNum, pageSize);
            return Result.success(page);
        } catch (Exception e) {
            log.error("分页查询保单失败", e);
            return Result.error("分页查询保单失败: " + e.getMessage());
        }
    }

    @Operation(summary = "根据保单号查询")
    @GetMapping("/{polNo}")
    public Result<PolInfoEntity> getByPolNo(@PathVariable String polNo) {
        try {
            return policyService.getByPolNo(polNo)
                    .map(Result::success)
                    .orElse(Result.error("保单不存在: " + polNo));
        } catch (Exception e) {
            log.error("查询保单失败", e);
            return Result.error("查询保单失败: " + e.getMessage());
        }
    }

    @Operation(summary = "根据投保人姓名查询")
    @GetMapping("/holder/{policyHolderName}")
    public Result<List<PolInfoEntity>> getByPolicyHolderName(@PathVariable String policyHolderName) {
        try {
            List<PolInfoEntity> policies = policyService.getByPolicyHolderName(policyHolderName);
            return Result.success(policies);
        } catch (Exception e) {
            log.error("查询保单失败", e);
            return Result.error("查询保单失败: " + e.getMessage());
        }
    }

    @Operation(summary = "根据身份证号查询")
    @GetMapping("/idcard/{idCardNo}")
    public Result<List<PolInfoEntity>> getByIdCardNo(@PathVariable String idCardNo) {
        try {
            List<PolInfoEntity> policies = policyService.getByIdCardNo(idCardNo);
            return Result.success(policies);
        } catch (Exception e) {
            log.error("查询保单失败", e);
            return Result.error("查询保单失败: " + e.getMessage());
        }
    }

    @Operation(summary = "根据用户ID查询")
    @GetMapping("/user/{userId}")
    public Result<List<PolInfoEntity>> getByUserId(@PathVariable String userId) {
        try {
            List<PolInfoEntity> policies = policyService.getByUserId(userId);
            return Result.success(policies);
        } catch (Exception e) {
            log.error("查询保单失败", e);
            return Result.error("查询保单失败: " + e.getMessage());
        }
    }

    @Operation(summary = "查询所有保单")
    @GetMapping("/all")
    public Result<List<PolInfoEntity>> getAllPolicies() {
        try {
            List<PolInfoEntity> policies = policyService.getAllPolicies();
            return Result.success(policies);
        } catch (Exception e) {
            log.error("查询所有保单失败", e);
            return Result.error("查询所有保单失败: " + e.getMessage());
        }
    }

    @Operation(summary = "新增保单")
    @PostMapping
    public Result<Boolean> createPolicy(@RequestBody PolInfoEntity entity) {
        try {
            boolean success = policyService.createPolicy(entity);
            return Result.success(success);
        } catch (Exception e) {
            log.error("新增保单失败", e);
            return Result.error("新增保单失败: " + e.getMessage());
        }
    }

    @Operation(summary = "更新保单")
    @PutMapping
    public Result<Boolean> updatePolicy(@RequestBody PolInfoEntity entity) {
        try {
            boolean success = policyService.updatePolicy(entity);
            return Result.success(success);
        } catch (Exception e) {
            log.error("更新保单失败", e);
            return Result.error("更新保单失败: " + e.getMessage());
        }
    }

    @Operation(summary = "删除保单")
    @DeleteMapping("/{polNo}")
    public Result<Boolean> deleteByPolNo(@PathVariable String polNo) {
        try {
            boolean success = policyService.deleteByPolNo(polNo);
            return Result.success(success);
        } catch (Exception e) {
            log.error("删除保单失败", e);
            return Result.error("删除保单失败: " + e.getMessage());
        }
    }
}
