package com.healthagent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.healthagent.dto.*;
import com.healthagent.entity.*;
import com.healthagent.mapper.*;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ExaminationService {

    @Autowired
    private ExaminationHospitalMapper hospitalMapper;

    @Autowired
    private ExaminationPackageMapper packageMapper;

    @Autowired
    private ExaminationPlanMapper planMapper;

    @Autowired
    private ExaminationBookingMapper bookingMapper;

    /**
     * 查询可用医院列表
     */
    public List<ExaminationHospitalDTO> getAvailableHospitals() {
        log.info("查询可用医院列表");
        LambdaQueryWrapper<ExaminationHospitalEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExaminationHospitalEntity::getStatus, 1)
               .orderByAsc(ExaminationHospitalEntity::getHospitalName);
        List<ExaminationHospitalEntity> entities = hospitalMapper.selectList(wrapper);
        return entities.stream().map(this::toHospitalDTO).collect(Collectors.toList());
    }

    /**
     * 搜索医院
     */
    public List<ExaminationHospitalDTO> searchHospitals(String keyword) {
        log.info("搜索医院: {}", keyword);
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAvailableHospitals();
        }
        LambdaQueryWrapper<ExaminationHospitalEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExaminationHospitalEntity::getStatus, 1)
               .and(w -> w.like(ExaminationHospitalEntity::getHospitalName, keyword)
                       .or().like(ExaminationHospitalEntity::getHospitalCode, keyword)
                       .or().like(ExaminationHospitalEntity::getHospitalLevel, keyword)
                       .or().like(ExaminationHospitalEntity::getDepartment, keyword))
               .orderByAsc(ExaminationHospitalEntity::getHospitalName);
        List<ExaminationHospitalEntity> entities = hospitalMapper.selectList(wrapper);
        return entities.stream().map(this::toHospitalDTO).collect(Collectors.toList());
    }

    /**
     * 获取医院详情
     */
    public Optional<ExaminationHospitalDTO> getHospitalByCode(String hospitalCode) {
        log.info("获取医院详情: {}", hospitalCode);
        LambdaQueryWrapper<ExaminationHospitalEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExaminationHospitalEntity::getHospitalCode, hospitalCode)
               .eq(ExaminationHospitalEntity::getStatus, 1);
        ExaminationHospitalEntity entity = hospitalMapper.selectOne(wrapper);
        return entity != null ? Optional.of(toHospitalDTO(entity)) : Optional.empty();
    }

    /**
     * 查询可用体检套餐列表
     */
    public List<ExaminationPackageDTO> getAvailablePackages() {
        log.info("查询可用体检套餐列表");
        LambdaQueryWrapper<ExaminationPackageEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExaminationPackageEntity::getStatus, 1)
               .orderByAsc(ExaminationPackageEntity::getPrice);
        List<ExaminationPackageEntity> entities = packageMapper.selectList(wrapper);
        return entities.stream().map(this::toPackageDTO).collect(Collectors.toList());
    }

    /**
     * 查询体检计划列表
     */
    public List<ExaminationPlanDTO> getAvailablePlans(Long hospitalId, Long packageId, LocalDate startDate, LocalDate endDate) {
        log.info("查询体检计划: hospitalId={}, packageId={}, startDate={}, endDate={}", hospitalId, packageId, startDate, endDate);
        LambdaQueryWrapper<ExaminationPlanEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExaminationPlanEntity::getStatus, 1)
               .ge(ExaminationPlanEntity::getScheduleDate, startDate != null ? startDate : LocalDate.now());
        if (hospitalId != null) {
            wrapper.eq(ExaminationPlanEntity::getHospitalId, hospitalId);
        }
        if (packageId != null) {
            wrapper.eq(ExaminationPlanEntity::getPackageId, packageId);
        }
        if (endDate != null) {
            wrapper.le(ExaminationPlanEntity::getScheduleDate, endDate);
        }
        wrapper.orderByAsc(ExaminationPlanEntity::getScheduleDate);
        List<ExaminationPlanEntity> entities = planMapper.selectList(wrapper);
        return entities.stream().map(this::toPlanDTO).collect(Collectors.toList());
    }

    /**
     * 体检预约
     */
    @Transactional(rollbackFor = Exception.class)
    public ExaminationBookingDTO bookExamination(ExaminationBookingRequestDTO request) {
        log.info("创建体检预约: userId={}, planId={}, bookerName={}", request.getUserId(), request.getPlanId(), request.getBookerName());

        ExaminationPlanEntity plan = planMapper.selectById(request.getPlanId());
        if (plan == null) {
            throw new RuntimeException("体检计划不存在");
        }
        if (plan.getStatus() != 1) {
            throw new RuntimeException("该体检计划不可预约");
        }
        if (plan.getAvailableSlots() <= 0) {
            throw new RuntimeException("该体检计划名额已满");
        }
        if (plan.getScheduleDate().isBefore(LocalDate.now())) {
            throw new RuntimeException("不能预约已过期的体检计划");
        }

        String bookingNo = generateBookingNo();
        ExaminationBookingEntity booking = new ExaminationBookingEntity();
        booking.setBookingNo(bookingNo);
        booking.setPlanId(request.getPlanId());
        booking.setUserId(request.getUserId());
        booking.setBookerName(request.getBookerName());
        booking.setBookerPhone(request.getBookerPhone());
        booking.setIdCardNo(request.getIdCardNo());
        booking.setNotes(request.getNotes());
        booking.setStatus("confirmed");
        bookingMapper.insert(booking);

        plan.setAvailableSlots(plan.getAvailableSlots() - 1);
        if (plan.getAvailableSlots() <= 0) {
            plan.setStatus(2);
        }
        planMapper.updateById(plan);

        log.info("体检预约成功: bookingNo={}", bookingNo);
        return toBookingDTO(booking, plan);
    }

    /**
     * 获取预约详情
     */
    public Optional<ExaminationBookingDTO> getBookingByNo(String bookingNo) {
        log.info("获取预约详情: {}", bookingNo);
        LambdaQueryWrapper<ExaminationBookingEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExaminationBookingEntity::getBookingNo, bookingNo);
        ExaminationBookingEntity booking = bookingMapper.selectOne(wrapper);
        if (booking == null) {
            return Optional.empty();
        }
        ExaminationPlanEntity plan = planMapper.selectById(booking.getPlanId());
        return Optional.of(toBookingDTO(booking, plan));
    }

    /**
     * 获取用户预约列表
     */
    public List<ExaminationBookingDTO> getUserBookings(String userId) {
        log.info("查询用户预约列表: {}", userId);
        LambdaQueryWrapper<ExaminationBookingEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExaminationBookingEntity::getUserId, userId)
               .orderByDesc(ExaminationBookingEntity::getCreateTime);
        List<ExaminationBookingEntity> bookings = bookingMapper.selectList(wrapper);
        return bookings.stream().map(b -> {
            ExaminationPlanEntity plan = planMapper.selectById(b.getPlanId());
            return toBookingDTO(b, plan);
        }).collect(Collectors.toList());
    }

    /**
     * 取消预约
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelBooking(String bookingNo, String userId) {
        log.info("取消预约: bookingNo={}, userId={}", bookingNo, userId);
        LambdaQueryWrapper<ExaminationBookingEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExaminationBookingEntity::getBookingNo, bookingNo)
               .eq(ExaminationBookingEntity::getUserId, userId);
        ExaminationBookingEntity booking = bookingMapper.selectOne(wrapper);
        if (booking == null) {
            return false;
        }
        if ("cancelled".equals(booking.getStatus())) {
            return false;
        }

        booking.setStatus("cancelled");
        bookingMapper.updateById(booking);

        ExaminationPlanEntity plan = planMapper.selectById(booking.getPlanId());
        if (plan != null) {
            plan.setAvailableSlots(plan.getAvailableSlots() + 1);
            if (plan.getStatus() == 2) {
                plan.setStatus(1);
            }
            planMapper.updateById(plan);
        }

        log.info("预约已取消: {}", bookingNo);
        return true;
    }

    /**
     * 预约须知
     */
    public String getBookingRequirements() {
        return """
            🏥 体检预约须知
            
            📅 预约时间：
            • 至少提前1天预约
            • 最多可预约未来3个月内的日期
            
            🏨 医院选择：
            • 支持多家三甲医院
            • 可通过医院名称或科室搜索
            
            📝 预约套餐：
            • 基础体检套餐
            • 全身体检套餐
            • 入职体检套餐
            • 老年体检套餐
            • 女性专项体检套餐
            
            📞 注意事项：
            • 体检前一天清淡饮食
            • 体检当天需空腹
            • 请携带身份证和预约凭证
            """;
    }

    private ExaminationHospitalDTO toHospitalDTO(ExaminationHospitalEntity entity) {
        ExaminationHospitalDTO dto = new ExaminationHospitalDTO();
        dto.setId(entity.getId());
        dto.setHospitalCode(entity.getHospitalCode());
        dto.setHospitalName(entity.getHospitalName());
        dto.setHospitalLevel(entity.getHospitalLevel());
        dto.setAddress(entity.getAddress());
        dto.setPhone(entity.getPhone());
        dto.setDepartment(entity.getDepartment());
        dto.setAvailableSlots(entity.getAvailableSlots());
        return dto;
    }

    private ExaminationPackageDTO toPackageDTO(ExaminationPackageEntity entity) {
        ExaminationPackageDTO dto = new ExaminationPackageDTO();
        dto.setId(entity.getId());
        dto.setPackageCode(entity.getPackageCode());
        dto.setPackageName(entity.getPackageName());
        dto.setPackageDesc(entity.getPackageDesc());
        dto.setPrice(entity.getPrice());
        dto.setDuration(entity.getDuration());
        return dto;
    }

    private ExaminationPlanDTO toPlanDTO(ExaminationPlanEntity entity) {
        ExaminationPlanDTO dto = new ExaminationPlanDTO();
        dto.setId(entity.getId());
        dto.setHospitalId(entity.getHospitalId());
        dto.setPackageId(entity.getPackageId());
        dto.setPlanName(entity.getPlanName());
        dto.setScheduleDate(entity.getScheduleDate());
        dto.setScheduleTime(entity.getScheduleTime());
        dto.setTotalSlots(entity.getTotalSlots());
        dto.setAvailableSlots(entity.getAvailableSlots());
        dto.setPrice(entity.getPrice());
        dto.setStatus(entity.getStatus());

        ExaminationHospitalEntity hospital = hospitalMapper.selectById(entity.getHospitalId());
        if (hospital != null) {
            dto.setHospitalName(hospital.getHospitalName());
        }
        ExaminationPackageEntity pkg = packageMapper.selectById(entity.getPackageId());
        if (pkg != null) {
            dto.setPackageName(pkg.getPackageName());
        }
        return dto;
    }

    private ExaminationBookingDTO toBookingDTO(ExaminationBookingEntity booking, ExaminationPlanEntity plan) {
        ExaminationBookingDTO dto = new ExaminationBookingDTO();
        dto.setId(booking.getId());
        dto.setBookingNo(booking.getBookingNo());
        dto.setPlanId(booking.getPlanId());
        dto.setUserId(booking.getUserId());
        dto.setBookerName(booking.getBookerName());
        dto.setBookerPhone(booking.getBookerPhone());
        dto.setIdCardNo(booking.getIdCardNo());
        dto.setNotes(booking.getNotes());
        dto.setStatus(booking.getStatus());
        dto.setCreateTime(booking.getCreateTime());

        if (plan != null) {
            dto.setPlanName(plan.getPlanName());
            dto.setScheduleDate(plan.getScheduleDate());
            dto.setScheduleTime(plan.getScheduleTime());
            dto.setPrice(plan.getPrice());
            ExaminationHospitalEntity hospital = hospitalMapper.selectById(plan.getHospitalId());
            if (hospital != null) {
                dto.setHospitalName(hospital.getHospitalName());
            }
            ExaminationPackageEntity pkg = packageMapper.selectById(plan.getPackageId());
            if (pkg != null) {
                dto.setPackageName(pkg.getPackageName());
            }
        }
        return dto;
    }

    private String generateBookingNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = String.format("%04d", new Random().nextInt(10000));
        return "EXM" + timestamp + random;
    }

    /**
     * 兼容旧接口：获取医院列表（供 SmartChatService 使用）
     */
    public List<ExaminationHospitalInfo> getAvailableHospitalsCompat() {
        return getAvailableHospitals().stream().map(h -> {
            ExaminationHospitalInfo info = new ExaminationHospitalInfo();
            info.setHospitalCode(h.getHospitalCode());
            info.setHospitalName(h.getHospitalName());
            info.setHospitalLevel(h.getHospitalLevel());
            info.setAddress(h.getAddress());
            info.setDepartment(h.getDepartment());
            return info;
        }).collect(Collectors.toList());
    }

    /**
     * 兼容旧接口：根据编码查医院详情
     */
    public Optional<ExaminationHospitalInfo> findHospitalByCode(String hospitalCode) {
        return getHospitalByCode(hospitalCode).map(h -> {
            ExaminationHospitalInfo info = new ExaminationHospitalInfo();
            info.setHospitalCode(h.getHospitalCode());
            info.setHospitalName(h.getHospitalName());
            info.setHospitalLevel(h.getHospitalLevel());
            info.setAddress(h.getAddress());
            info.setDepartment(h.getDepartment());
            return info;
        });
    }

    /**
     * 兼容旧接口：获取可用体检套餐（供 SmartChatService 使用）
     */
    public List<ExaminationPackageInfo> getAvailablePackagesCompat() {
        return getAvailablePackages().stream().map(p -> {
            ExaminationPackageInfo info = new ExaminationPackageInfo();
            info.setPackageCode(p.getPackageCode());
            info.setPackageName(p.getPackageName());
            info.setPrice(p.getPrice());
            info.setDuration(p.getDuration());
            return info;
        }).collect(Collectors.toList());
    }

    /**
     * 兼容旧接口：根据名称查找套餐
     */
    public Optional<ExaminationPackageInfo> findPackageByName(String packageName) {
        List<ExaminationPackageInfo> packages = getAvailablePackagesCompat();
        return packages.stream()
                .filter(p -> p.getPackageName().contains(packageName))
                .findFirst();
    }

    /**
     * 兼容旧接口：查询体检计划（供 SmartChatService 使用）
     */
    public List<ExaminationPlanInfo> findAvailablePlans(ExaminationBookingRequest request) {
        LocalDate targetDate = request.getExaminationDate() != null ? LocalDate.parse(request.getExaminationDate()) : LocalDate.now();

        List<ExaminationPlanInfo> result = new ArrayList<>();

        Long hospitalId = null;
        if (request.getHospitalCode() != null) {
            Optional<ExaminationHospitalEntity> opt = Optional.ofNullable(
                    hospitalMapper.selectOne(new LambdaQueryWrapper<ExaminationHospitalEntity>()
                            .eq(ExaminationHospitalEntity::getHospitalCode, request.getHospitalCode())));
            if (opt.isPresent()) {
                hospitalId = opt.get().getId();
            }
        }

        Long packageId = null;
        if (request.getPackageName() != null) {
            Optional<ExaminationPackageEntity> opt = Optional.ofNullable(
                    packageMapper.selectOne(new LambdaQueryWrapper<ExaminationPackageEntity>()
                            .like(ExaminationPackageEntity::getPackageName, request.getPackageName())));
            if (opt.isPresent()) {
                packageId = opt.get().getId();
            }
        }

        List<ExaminationPlanDTO> plans = getAvailablePlans(hospitalId, packageId, targetDate, targetDate.plusDays(30));
        for (ExaminationPlanDTO p : plans) {
            ExaminationPlanInfo info = new ExaminationPlanInfo();
            info.setId(p.getId());
            info.setHospitalName(p.getHospitalName());
            info.setPackageName(p.getPackageName());
            info.setExaminationDate(p.getScheduleDate().toString());
            info.setExaminationTime(p.getScheduleTime());
            info.setAvailableSlots(p.getAvailableSlots());
            info.setPrice(p.getPrice());
            result.add(info);
        }
        return result;
    }

    /**
     * 兼容旧接口：体检预约（供 SmartChatService 使用）
     */
    public ExaminationBooking bookExamination(ExaminationBookingRequest request) {
        ExaminationBookingRequestDTO dto = new ExaminationBookingRequestDTO();
        dto.setPlanId(request.getPlanId() != null ? request.getPlanId() : findFirstAvailablePlanId(request));
        dto.setUserId(request.getUserId());
        dto.setBookerName(request.getBookerName() != null ? request.getBookerName() : "未知");
        dto.setBookerPhone(request.getBookerPhone() != null ? request.getBookerPhone() : "13800138000");
        dto.setIdCardNo(request.getIdCardNo());
        dto.setNotes(request.getNotes());
        ExaminationBookingDTO bookingDTO = bookExamination(dto);
        ExaminationBooking booking = new ExaminationBooking();
        booking.setBookingNo(bookingDTO.getBookingNo());
        booking.setHospitalName(bookingDTO.getHospitalName());
        booking.setPackageName(bookingDTO.getPackageName());
        booking.setExaminationDate(bookingDTO.getScheduleDate().toString());
        booking.setExaminationTime(bookingDTO.getScheduleTime());
        booking.setStatus(bookingDTO.getStatus());
        return booking;
    }

    private Long findFirstAvailablePlanId(ExaminationBookingRequest request) {
        LocalDate targetDate = request.getExaminationDate() != null ? LocalDate.parse(request.getExaminationDate()) : LocalDate.now();
        List<ExaminationPlanInfo> plans = findAvailablePlans(request);
        for (ExaminationPlanInfo p : plans) {
            if (p.getAvailableSlots() > 0 && !LocalDate.parse(p.getExaminationDate()).isBefore(targetDate)) {
                return p.getId();
            }
        }
        throw new RuntimeException("找不到可预约的体检计划");
    }

    /**
     * 兼容旧接口：格式化预约信息为文本
     */
    public String formatBookingAsText(ExaminationBooking booking) {
        StringBuilder sb = new StringBuilder();
        sb.append("🏥 预约成功\n");
        sb.append("━━━━━━━━━━━━━━━━\n");
        sb.append("📋 预约编号: ").append(booking.getBookingNo()).append("\n");
        sb.append("🏨 医院名称: ").append(booking.getHospitalName()).append("\n");
        sb.append("📦 体检套餐: ").append(booking.getPackageName()).append("\n");
        sb.append("📅 体检日期: ").append(booking.getExaminationDate()).append("\n");
        sb.append("⏰ 体检时间: ").append(booking.getExaminationTime()).append("\n");
        sb.append("📊 状态: ").append("confirmed".equals(booking.getStatus()) ? "✅ 已确认" : booking.getStatus()).append("\n");
        sb.append("━━━━━━━━━━━━━━━━\n\n");
        sb.append("📞 注意事项:\n");
        sb.append("• 体检前一天清淡饮食\n");
        sb.append("• 体检当天需空腹\n");
        sb.append("• 请携带身份证和预约凭证\n");
        return sb.toString();
    }

    /**
     * 兼容旧接口：体检医院信息
     */
    @Data
    public static class ExaminationHospitalInfo {
        private String hospitalCode;
        private String hospitalName;
        private String hospitalLevel;
        private String address;
        private String department;
    }

    /**
     * 兼容旧接口：体检套餐信息
     */
    @Data
    public static class ExaminationPackageInfo {
        private String packageCode;
        private String packageName;
        private BigDecimal price;
        private String duration;
    }

    /**
     * 兼容旧接口：体检计划信息
     */
    @Data
    public static class ExaminationPlanInfo {
        private Long id;
        private String hospitalName;
        private String packageName;
        private String examinationDate;
        private String examinationTime;
        private Integer availableSlots;
        private BigDecimal price;
    }

    /**
     * 兼容旧接口：体检预约信息
     */
    @Data
    public static class ExaminationBooking {
        private String bookingNo;
        private String hospitalName;
        private String packageName;
        private String examinationDate;
        private String examinationTime;
        private String status;
    }

    /**
     * 兼容旧接口：体检预约请求
     */
    @Data
    public static class ExaminationBookingRequest {
        private String userId;
        private String hospitalName;
        private String hospitalCode;
        private String packageName;
        private String examinationDate;
        private String examinationTime;
        private String bookerName;
        private String bookerPhone;
        private String idCardNo;
        private String notes;
        private Long planId;
    }
}
