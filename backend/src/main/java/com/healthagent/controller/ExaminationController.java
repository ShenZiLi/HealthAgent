package com.healthagent.controller;

import com.healthagent.common.Result;
import com.healthagent.dto.ExaminationBooking;
import com.healthagent.dto.ExaminationBookingRequest;
import com.healthagent.dto.HospitalInfo;
import com.healthagent.service.ExaminationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "体检预约接口")
@RestController
@RequestMapping("/api/examination")
public class ExaminationController {

    @Autowired
    private ExaminationService examinationService;

    @Operation(summary = "获取可用医院列表")
    @GetMapping("/hospitals")
    public Result<List<HospitalInfo>> getAvailableHospitals() {
        try {
            List<HospitalInfo> hospitals = examinationService.getAvailableHospitals();
            return Result.success(hospitals);
        } catch (Exception e) {
            log.error("获取医院列表失败", e);
            return Result.error("获取医院列表失败: " + e.getMessage());
        }
    }

    @Operation(summary = "搜索医院")
    @GetMapping("/hospitals/search")
    public Result<List<HospitalInfo>> searchHospitals(@RequestParam String keyword) {
        try {
            List<HospitalInfo> hospitals = examinationService.searchHospitals(keyword);
            return Result.success(hospitals);
        } catch (Exception e) {
            log.error("搜索医院失败", e);
            return Result.error("搜索医院失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取医院详情")
    @GetMapping("/hospitals/{hospitalCode}")
    public Result<HospitalInfo> getHospitalDetail(@PathVariable String hospitalCode) {
        try {
            return examinationService.getHospitalByCode(hospitalCode)
                .map(Result::success)
                .orElse(Result.error("医院不存在"));
        } catch (Exception e) {
            log.error("获取医院详情失败", e);
            return Result.error("获取医院详情失败: " + e.getMessage());
        }
    }

    @Operation(summary = "创建体检预约")
    @PostMapping("/book")
    public Result<ExaminationBooking> bookExamination(@RequestBody ExaminationBookingRequest request) {
        try {
            if (request.getUserId() == null || request.getUserId().trim().isEmpty()) {
                return Result.error("用户ID不能为空");
            }
            
            if (request.getHospitalName() == null || request.getHospitalName().trim().isEmpty()) {
                return Result.error("医院名称不能为空");
            }
            
            if (request.getExaminationDate() == null || request.getExaminationDate().trim().isEmpty()) {
                return Result.error("体检日期不能为空");
            }
            
            if (!examinationService.isValidBookingDate(request.getExaminationDate())) {
                return Result.error("体检日期无效，请选择未来3个月内的日期");
            }
            
            ExaminationBooking booking = examinationService.bookExamination(request);
            return Result.success(booking);
        } catch (Exception e) {
            log.error("创建体检预约失败", e);
            return Result.error("创建体检预约失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取预约详情")
    @GetMapping("/booking/{bookingId}")
    public Result<ExaminationBooking> getBookingDetail(@PathVariable String bookingId) {
        try {
            return examinationService.getBookingById(bookingId)
                .map(Result::success)
                .orElse(Result.error("预约不存在"));
        } catch (Exception e) {
            log.error("获取预约详情失败", e);
            return Result.error("获取预约详情失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取用户预约列表")
    @GetMapping("/bookings/{userId}")
    public Result<List<ExaminationBooking>> getUserBookings(@PathVariable String userId) {
        try {
            List<ExaminationBooking> bookings = examinationService.getUserBookings(userId);
            return Result.success(bookings);
        } catch (Exception e) {
            log.error("获取用户预约列表失败", e);
            return Result.error("获取用户预约列表失败: " + e.getMessage());
        }
    }

    @Operation(summary = "取消预约")
    @DeleteMapping("/booking/{bookingId}")
    public Result<Void> cancelBooking(
        @PathVariable String bookingId,
        @RequestParam String userId
    ) {
        try {
            boolean success = examinationService.cancelBooking(bookingId, userId);
            if (success) {
                return Result.success();
            } else {
                return Result.error("取消预约失败，请检查预约ID或权限");
            }
        } catch (Exception e) {
            log.error("取消预约失败", e);
            return Result.error("取消预约失败: " + e.getMessage());
        }
    }

    @Operation(summary = "改签预约")
    @PutMapping("/booking/{bookingId}/reschedule")
    public Result<ExaminationBooking> rescheduleBooking(
        @PathVariable String bookingId,
        @RequestParam String userId,
        @RequestParam String newDate,
        @RequestParam(required = false) String newTime
    ) {
        try {
            if (!examinationService.isValidBookingDate(newDate)) {
                return Result.error("改签日期无效，请选择未来3个月内的日期");
            }
            
            boolean success = examinationService.rescheduleBooking(bookingId, userId, newDate, newTime);
            if (success) {
                return examinationService.getBookingById(bookingId)
                    .map(Result::success)
                    .orElse(Result.error("预约不存在"));
            } else {
                return Result.error("改签预约失败，请检查预约ID或权限");
            }
        } catch (Exception e) {
            log.error("改签预约失败", e);
            return Result.error("改签预约失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取预约须知")
    @GetMapping("/requirements")
    public Result<String> getBookingRequirements() {
        try {
            String requirements = examinationService.getBookingRequirements();
            return Result.success(requirements);
        } catch (Exception e) {
            log.error("获取预约须知失败", e);
            return Result.error("获取预约须知失败: " + e.getMessage());
        }
    }
}
