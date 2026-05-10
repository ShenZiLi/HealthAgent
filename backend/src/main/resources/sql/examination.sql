-- 医院信息表
CREATE TABLE IF NOT EXISTS `examination_hospital` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `hospital_code` VARCHAR(64) NOT NULL COMMENT '医院编码',
  `hospital_name` VARCHAR(128) NOT NULL COMMENT '医院名称',
  `hospital_level` VARCHAR(64) COMMENT '医院等级',
  `address` VARCHAR(256) COMMENT '医院地址',
  `phone` VARCHAR(32) COMMENT '联系电话',
  `department` VARCHAR(128) COMMENT '体检科室',
  `available_slots` INT DEFAULT 0 COMMENT '可预约名额',
  `status` TINYINT DEFAULT 1 COMMENT '状态(0-停用 1-启用)',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_hospital_code` (`hospital_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='医院信息表';

-- 体检套餐表
CREATE TABLE IF NOT EXISTS `examination_package` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `package_code` VARCHAR(64) NOT NULL COMMENT '套餐编码',
  `package_name` VARCHAR(128) NOT NULL COMMENT '套餐名称',
  `package_desc` TEXT COMMENT '套餐描述',
  `price` DECIMAL(10, 2) COMMENT '价格',
  `duration` VARCHAR(32) COMMENT '预计时长(分钟)',
  `status` TINYINT DEFAULT 1 COMMENT '状态(0-停用 1-启用)',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_package_code` (`package_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='体检套餐表';

-- 体检预约表
CREATE TABLE IF NOT EXISTS `examination_booking` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `booking_no` VARCHAR(64) NOT NULL COMMENT '预约编号',
  `hospital_id` BIGINT NOT NULL COMMENT '医院ID',
  `package_id` BIGINT NOT NULL COMMENT '套餐ID',
  `schedule_date` DATE NOT NULL COMMENT '预约日期',
  `user_id` VARCHAR(64) NOT NULL COMMENT '用户ID',
  `booker_name` VARCHAR(128) NOT NULL COMMENT '登记人姓名',
  `booker_phone` VARCHAR(32) NOT NULL COMMENT '登记人电话',
  `id_card_no` VARCHAR(18) COMMENT '身份证号',
  `notes` TEXT COMMENT '备注',
  `status` VARCHAR(32) DEFAULT 'confirmed' COMMENT '预约状态(confirmed-已确认/cancelled-已取消/completed-已完成)',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_booking_no` (`booking_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_hospital_id` (`hospital_id`),
  KEY `idx_package_id` (`package_id`),
  KEY `idx_schedule_date` (`schedule_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='体检预约表';

-- 初始化医院数据
INSERT INTO `examination_hospital` (`hospital_code`, `hospital_name`, `hospital_level`, `address`, `phone`, `department`, `available_slots`, `status`) VALUES
('PEKING_UNION', '北京协和医院', '三级甲等', '北京市东城区帅府园1号', '010-69156114', '体检中心', 15, 1),
('PEKING_301', '301医院', '三级甲等', '北京市海淀区复兴路28号', '010-68182255', '健康管理中心', 20, 1),
('PEKING_FIRST', '北京大学第一医院', '三级甲等', '北京市西城区西什库大街8号', '010-83572211', '体检科', 12, 1),
('SHANGHAI_ZHONGSHAN', '复旦大学附属中山医院', '三级甲等', '上海市徐汇区枫林路180号', '021-64041990', '健康体检中心', 18, 1),
('SHANGHAI_RUIJIN', '上海交通大学医学院附属瑞金医院', '三级甲等', '上海市黄浦区瑞金二路197号', '021-64370045', '体检中心', 16, 1),
('GUANGDONG_GENERAL', '广东省人民医院', '三级甲等', '广州市越秀区中山二路106号', '020-83827812', '健康管理中心', 22, 1),
('WEST_CHINA', '四川大学华西医院', '三级甲等', '成都市武侯区国学巷37号', '028-85422286', '健康体检中心', 25, 1),
('WUHAN_TONGJI', '武汉同济医院', '三级甲等', '武汉市硚口区解放大道1095号', '027-83663600', '体检中心', 19, 1);

-- 初始化体检套餐数据
INSERT INTO `examination_package` (`package_code`, `package_name`, `package_desc`, `price`, `duration`, `status`) VALUES
('PKG_BASIC', '基础体检套餐', '身高、体重、血压、血常规、尿常规、肝功能、肾功能、心电图', 299.00, '60', 1),
('PKG_FULL', '全身体检套餐', '基础体检项目+彩超、CT、肿瘤标志物、甲状腺功能、血糖血脂全套', 899.00, '120', 1),
('PKG_EMPLOYMENT', '入职体检套餐', '身高、体重、视力、听力、血常规、肝功能、胸片、心电图', 199.00, '45', 1),
('PKG_SENIOR', '老年体检套餐', '全身体检项目+骨密度、颈动脉彩超、眼底检查、前列腺/乳腺彩超', 1299.00, '150', 1),
('PKG_WOMAN', '女性专项体检套餐', '基础体检+妇科检查、乳腺彩超、宫颈TCT、HPV检查', 799.00, '90', 1);