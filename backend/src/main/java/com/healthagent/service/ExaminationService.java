package com.healthagent.service;

import com.healthagent.dto.ExaminationBooking;
import com.healthagent.dto.ExaminationBookingRequest;
import com.healthagent.dto.HospitalInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class ExaminationService {

    private static final Map<String, ExaminationBooking> BOOKINGS = new ConcurrentHashMap<>();
    
    private static final List<HospitalInfo> MOCK_HOSPITALS = new ArrayList<>();

    static {
        MOCK_HOSPITALS.add(new HospitalInfo(
            "PEKING_UNION_HOSPITAL",
            "北京协和医院",
            "三级甲等",
            "北京市东城区帅府园1号",
            "010-69156114",
            "体检中心",
            15
        ));
        
        MOCK_HOSPITALS.add(new HospitalInfo(
            "301_HOSPITAL",
            "301医院",
            "三级甲等",
            "北京市海淀区复兴路28号",
            "010-68182255",
            "健康管理中心",
            20
        ));
        
        MOCK_HOSPITALS.add(new HospitalInfo(
            "PEKING_FIRST_HOSPITAL",
            "北京大学第一医院",
            "三级甲等",
            "北京市西城区西什库大街8号",
            "010-83572211",
            "体检科",
            12
        ));
        
        MOCK_HOSPITALS.add(new HospitalInfo(
            "Zhongshan_Hospital",
            "复旦大学附属中山医院",
            "三级甲等",
            "上海市徐汇区枫林路180号",
            "021-64041990",
            "健康体检中心",
            18
        ));
        
        MOCK_HOSPITALS.add(new HospitalInfo(
            "Ruijin_Hospital",
            "上海交通大学医学院附属瑞金医院",
            "三级甲等",
            "上海市黄浦区瑞金二路197号",
            "021-64370045",
            "体检中心",
            16
        ));
        
        MOCK_HOSPITALS.add(new HospitalInfo(
            "Guangdong_General_Hospital",
            "广东省人民医院",
            "三级甲等",
            "广州市越秀区中山二路106号",
            "020-83827812",
            "健康管理中心",
            22
        ));
        
        MOCK_HOSPITALS.add(new HospitalInfo(
            "West_China_Hospital",
            "四川大学华西医院",
            "三级甲等",
            "成都市武侯区国学巷37号",
            "028-85422286",
            "健康体检中心",
            25
        ));
        
        MOCK_HOSPITALS.add(new HospitalInfo(
            "Tongji_Hospital",
            "武汉同济医院",
            "三级甲等",
            "武汉市硚口区解放大道1095号",
            "027-83663600",
            "体检中心",
            19
        ));
    }

    public List<HospitalInfo> getAvailableHospitals() {
        log.info("查询可用医院列表");
        
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        return new ArrayList<>(MOCK_HOSPITALS);
    }

    public List<HospitalInfo> searchHospitals(String keyword) {
        log.info("搜索医院: {}", keyword);
        
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAvailableHospitals();
        }
        
        String lowerKeyword = keyword.toLowerCase();
        return MOCK_HOSPITALS.stream()
            .filter(hospital -> 
                hospital.getHospitalName().toLowerCase().contains(lowerKeyword) ||
                hospital.getHospitalCode().toLowerCase().contains(lowerKeyword) ||
                hospital.getHospitalLevel().toLowerCase().contains(lowerKeyword) ||
                hospital.getDepartment().toLowerCase().contains(lowerKeyword)
            )
            .toList();
    }

    public Optional<HospitalInfo> getHospitalByCode(String hospitalCode) {
        return MOCK_HOSPITALS.stream()
            .filter(h -> h.getHospitalCode().equals(hospitalCode))
            .findFirst();
    }

    public ExaminationBooking bookExamination(ExaminationBookingRequest request) {
        log.info("创建体检预约: userId={}, hospital={}, date={}", 
            request.getUserId(), request.getHospitalName(), request.getExaminationDate());
        
        try {
            Thread.sleep(800);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        String bookingId = generateBookingId();
        
        ExaminationBooking booking = new ExaminationBooking();
        booking.setBookingId(bookingId);
        booking.setUserId(request.getUserId());
        booking.setHospitalName(request.getHospitalName());
        booking.setHospitalCode(request.getHospitalCode());
        booking.setExaminationDate(request.getExaminationDate());
        booking.setExaminationTime(request.getExaminationTime() != null ? request.getExaminationTime() : "上午 9:00");
        booking.setPackageName(request.getPackageName() != null ? request.getPackageName() : "全身体检套餐");
        booking.setContactPhone(request.getContactPhone());
        booking.setNotes(request.getNotes());
        booking.setStatus("confirmed");
        
        BOOKINGS.put(bookingId, booking);
        
        log.info("体检预约成功: bookingId={}", bookingId);
        
        return booking;
    }

    public Optional<ExaminationBooking> getBookingById(String bookingId) {
        return Optional.ofNullable(BOOKINGS.get(bookingId));
    }

    public List<ExaminationBooking> getUserBookings(String userId) {
        log.info("查询用户 {} 的体检预约", userId);
        
        try {
            Thread.sleep(400);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        return BOOKINGS.values().stream()
            .filter(booking -> booking.getUserId().equals(userId))
            .sorted(Comparator.comparing(ExaminationBooking::getExaminationDate).reversed())
            .toList();
    }

    public boolean cancelBooking(String bookingId, String userId) {
        log.info("取消体检预约: bookingId={}, userId={}", bookingId, userId);
        
        ExaminationBooking booking = BOOKINGS.get(bookingId);
        if (booking == null) {
            log.warn("预约不存在: {}", bookingId);
            return false;
        }
        
        if (!booking.getUserId().equals(userId)) {
            log.warn("无权取消该预约: {}", bookingId);
            return false;
        }
        
        LocalDate bookingDate = LocalDate.parse(booking.getExaminationDate());
        if (bookingDate.isBefore(LocalDate.now())) {
            log.warn("无法取消已过期的预约: {}", bookingId);
            return false;
        }
        
        booking.setStatus("cancelled");
        log.info("体检预约已取消: {}", bookingId);
        return true;
    }

    public boolean rescheduleBooking(String bookingId, String userId, String newDate, String newTime) {
        log.info("改签体检预约: bookingId={}, userId={}, newDate={}, newTime={}", 
            bookingId, userId, newDate, newTime);
        
        ExaminationBooking booking = BOOKINGS.get(bookingId);
        if (booking == null) {
            log.warn("预约不存在: {}", bookingId);
            return false;
        }
        
        if (!booking.getUserId().equals(userId)) {
            log.warn("无权改签该预约: {}", bookingId);
            return false;
        }
        
        LocalDate bookingDate = LocalDate.parse(booking.getExaminationDate());
        if (bookingDate.isBefore(LocalDate.now())) {
            log.warn("无法改签已过期的预约: {}", bookingId);
            return false;
        }
        
        booking.setExaminationDate(newDate);
        if (newTime != null) {
            booking.setExaminationTime(newTime);
        }
        
        log.info("体检预约改签成功: {}", bookingId);
        return true;
    }

    public String formatBookingAsText(ExaminationBooking booking) {
        StringBuilder sb = new StringBuilder();
        sb.append("📋 体检预约信息\n");
        sb.append("━━━━━━━━━━━━━━━━\n");
        sb.append("预约编号：").append(booking.getBookingId()).append("\n");
        sb.append("医院名称：").append(booking.getHospitalName()).append("\n");
        sb.append("预约日期：").append(booking.getExaminationDate()).append("\n");
        sb.append("预约时间：").append(booking.getExaminationTime()).append("\n");
        sb.append("体检套餐：").append(booking.getPackageName()).append("\n");
        sb.append("预约状态：");
        
        switch (booking.getStatus()) {
            case "confirmed" -> sb.append("✅ 已确认");
            case "cancelled" -> sb.append("❌ 已取消");
            case "completed" -> sb.append("✔️ 已完成");
            default -> sb.append(booking.getStatus());
        }
        sb.append("\n");
        
        if (booking.getContactPhone() != null) {
            sb.append("联系电话：").append(booking.getContactPhone()).append("\n");
        }
        if (booking.getNotes() != null && !booking.getNotes().isEmpty()) {
            sb.append("备注：").append(booking.getNotes()).append("\n");
        }
        
        sb.append("━━━━━━━━━━━━━━━━\n");
        return sb.toString();
    }

    private String generateBookingId() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = String.format("%04d", new Random().nextInt(10000));
        return "EXM" + timestamp + random;
    }

    public boolean isValidBookingDate(String date) {
        try {
            LocalDate bookingDate = LocalDate.parse(date);
            LocalDate today = LocalDate.now();
            
            return !bookingDate.isBefore(today) && 
                   !bookingDate.isAfter(today.plusMonths(3));
        } catch (Exception e) {
            return false;
        }
    }

    public String getBookingRequirements() {
        return """
            🏥 体检预约须知
            
            📅 预约时间：
            • 至少提前1天预约
            • 最多可预约未来3个月内的日期
            
            🏨 医院选择：
            • 支持8家三甲医院
            • 可通过医院名称或科室搜索
            
            📝 预约套餐：
            • 全身体检套餐
            • 入职体检套餐
            • 专项体检套餐
            
            📞 注意事项：
            • 体检前一天清淡饮食
            • 体检当天需空腹
            • 请携带身份证和预约凭证
            """;
    }
}
