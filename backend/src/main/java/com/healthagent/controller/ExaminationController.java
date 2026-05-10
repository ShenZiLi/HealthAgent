package com.healthagent.controller;

import com.healthagent.common.Result;
import com.healthagent.dto.*;
import com.healthagent.service.ExaminationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * 体检预约控制器
 */
@Tag(name = "体检预约管理")
@RestController
@RequestMapping("/api/examinations")
@Slf4j
public class ExaminationController {

    @Autowired
    private ExaminationService examinationService;

    @Operation(summary = "获取可用医院列表")
    @GetMapping("/hospitals")
    public Result<List<ExaminationHospitalDTO>> getHospitals(@RequestParam(required = false) String keyword) {
        try {
            List<ExaminationHospitalDTO> hospitals;
            if (keyword != null && !keyword.trim().isEmpty()) {
                hospitals = examinationService.searchHospitals(keyword);
            } else {
                hospitals = examinationService.getAvailableHospitals();
            }
            return Result.success(hospitals);
        } catch (Exception e) {
            log.error("查询医院列表失败", e);
            return Result.error("查询医院列表失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取医院详情")
    @GetMapping("/hospitals/{hospitalCode}")
    public Result<ExaminationHospitalDTO> getHospital(@PathVariable String hospitalCode) {
        try {
            Optional<ExaminationHospitalDTO> hospital = examinationService.getHospitalByCode(hospitalCode);
            return hospital.map(Result::success)
                    .orElse(Result.error("医院不存在"));
        } catch (Exception e) {
            log.error("查询医院详情失败", e);
            return Result.error("查询医院详情失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取可用体检套餐列表")
    @GetMapping("/packages")
    public Result<List<ExaminationPackageDTO>> getPackages() {
        try {
            List<ExaminationPackageDTO> packages = examinationService.getAvailablePackages();
            return Result.success(packages);
        } catch (Exception e) {
            log.error("查询套餐列表失败", e);
            return Result.error("查询套餐列表失败: " + e.getMessage());
        }
    }

    @Operation(summary = "体检预约")
    @PostMapping("/book")
    public Result<ExaminationBookingDTO> book(@Valid @RequestBody ExaminationBookingRequestDTO request) {
        try {
            ExaminationBookingDTO booking = examinationService.bookExamination(request);
            return Result.success(booking);
        } catch (RuntimeException e) {
            log.warn("体检预约失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("体检预约失败", e);
            return Result.error("预约失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取预约详情")
    @GetMapping("/bookings/{bookingNo}")
    public Result<ExaminationBookingDTO> getBooking(@PathVariable String bookingNo) {
        try {
            Optional<ExaminationBookingDTO> booking = examinationService.getBookingByNo(bookingNo);
            return booking.map(Result::success)
                    .orElse(Result.error("预约不存在"));
        } catch (Exception e) {
            log.error("查询预约详情失败", e);
            return Result.error("查询预约详情失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取用户预约列表")
    @GetMapping("/users/{userId}/bookings")
    public Result<List<ExaminationBookingDTO>> getUserBookings(@PathVariable String userId) {
        try {
            List<ExaminationBookingDTO> bookings = examinationService.getUserBookings(userId);
            return Result.success(bookings);
        } catch (Exception e) {
            log.error("查询用户预约列表失败", e);
            return Result.error("查询用户预约列表失败: " + e.getMessage());
        }
    }

    @Operation(summary = "取消预约")
    @DeleteMapping("/bookings/{bookingNo}")
    public Result<Boolean> cancelBooking(@PathVariable String bookingNo, @RequestParam String userId) {
        try {
            boolean success = examinationService.cancelBooking(bookingNo, userId);
            return success ? Result.success(true) : Result.error("取消预约失败");
        } catch (Exception e) {
            log.error("取消预约失败", e);
            return Result.error("取消预约失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取预约须知")
    @GetMapping("/requirements")
    public Result<String> getRequirements() {
        return Result.success(examinationService.getBookingRequirements());
    }
}
