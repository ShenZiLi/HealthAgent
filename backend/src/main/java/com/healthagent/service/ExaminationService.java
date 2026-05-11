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
     * 体检预约
     */
    @Transactional(rollbackFor = Exception.class)
    public ExaminationBookingDTO bookExamination(ExaminationBookingRequestDTO request) {
        log.info("创建体检预约: userId={}, hospitalId={}, packageId={}, scheduleDate={}",
                request.getUserId(), request.getHospitalId(), request.getPackageId(), request.getScheduleDate());

        String bookingNo = generateBookingNo();
        ExaminationBookingEntity booking = new ExaminationBookingEntity();
        booking.setBookingNo(bookingNo);
        booking.setHospitalId(request.getHospitalId());
        booking.setPackageId(request.getPackageId());
        booking.setScheduleDate(request.getScheduleDate());
        booking.setUserId(request.getUserId());
        booking.setBookerName(request.getBookerName());
        booking.setBookerPhone(request.getBookerPhone());
        booking.setIdCardNo(request.getIdCardNo());
        booking.setNotes(request.getNotes());
        booking.setStatus("confirmed");
        bookingMapper.insert(booking);

        log.info("体检预约成功: bookingNo={}", bookingNo);
        return toBookingDTO(booking, null, null);
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
        ExaminationHospitalEntity hospital = hospitalMapper.selectById(booking.getHospitalId());
        ExaminationPackageEntity pkg = packageMapper.selectById(booking.getPackageId());
        return Optional.of(toBookingDTO(booking, hospital, pkg));
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
            ExaminationHospitalEntity hospital = b.getHospitalId() != null ? hospitalMapper.selectById(b.getHospitalId()) : null;
            ExaminationPackageEntity pkg = packageMapper.selectById(b.getPackageId());
            return toBookingDTO(b, hospital, pkg);
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
    
    private ExaminationBookingDTO toBookingDTO(ExaminationBookingEntity booking, ExaminationHospitalEntity hospital, ExaminationPackageEntity pkg) {
        ExaminationBookingDTO dto = new ExaminationBookingDTO();
        dto.setId(booking.getId());
        dto.setBookingNo(booking.getBookingNo());
        dto.setUserId(booking.getUserId());
        dto.setBookerName(booking.getBookerName());
        dto.setBookerPhone(booking.getBookerPhone());
        dto.setIdCardNo(booking.getIdCardNo());
        dto.setNotes(booking.getNotes());
        dto.setStatus(booking.getStatus());
        dto.setCreateTime(booking.getCreateTime());
        return dto;
    }

    private String generateBookingNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = String.format("%04d", new Random().nextInt(10000));
        return "EXM" + timestamp + random;
    }

}
