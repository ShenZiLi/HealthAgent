-- 保单信息表
CREATE TABLE IF NOT EXISTS `pol_info` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `pol_no` VARCHAR(64) NOT NULL COMMENT '保单号',
  `policy_holder_name` VARCHAR(128) NOT NULL COMMENT '投保人姓名',
  `insured_name` VARCHAR(128) NOT NULL COMMENT '被保险人姓名',
  `id_card_no` VARCHAR(18) NOT NULL COMMENT '身份证号',
  `insurance_company` VARCHAR(128) NOT NULL COMMENT '保险公司',
  `product_name` VARCHAR(256) NOT NULL COMMENT '产品名称',
  `insurance_type` VARCHAR(64) COMMENT '保险类型(健康险/寿险/意外险/医疗险)',
  `premium_amount` DECIMAL(10, 2) COMMENT '保费金额',
  `insured_amount` DECIMAL(12, 2) COMMENT '保额',
  `status` VARCHAR(32) DEFAULT 'active' COMMENT '保单状态(active-有效/expired-已过期/cancelled-已取消)',
  `effective_date` DATETIME COMMENT '生效日期',
  `expiry_date` DATETIME COMMENT '到期日期',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除标记(0-未删除 1-已删除)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pol_no` (`pol_no`),
  KEY `idx_policy_holder_name` (`policy_holder_name`),
  KEY `idx_insurance_company` (`insurance_company`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='保单信息表';

-- 初始化保单数据
INSERT INTO `pol_info` (`pol_no`, `policy_holder_name`, `insured_name`, `id_card_no`, `insurance_company`, `product_name`, `insurance_type`, `premium_amount`, `insured_amount`, `status`, `effective_date`, `expiry_date`) VALUES
('POL20240001', '张三', '张三', '110101199001011234', '平安保险', '健康医疗保险 Plus', '健康险', 3650.00, 500000.00, 'active', '2024-01-01 00:00:00', '2026-12-31 23:59:59'),
('POL20240002', '张三', '张三', '110101199001011234', '中国人寿', '终身寿险尊享版', '寿险', 12000.00, 1000000.00, 'active', '2023-06-01 00:00:00', NULL),
('POL20230015', '张三', '张三', '110101199001011234', '太平洋保险', '意外伤害保险A款', '意外险', 280.00, 100000.00, 'expired', '2022-01-01 00:00:00', '2023-12-31 23:59:59'),
('POL20240015', '李四', '李四', '310101198505052345', '友邦保险', '重大疾病保险尊享版', '健康险', 5800.00, 800000.00, 'active', '2024-03-15 00:00:00', '2027-03-14 23:59:59'),
('POL20240020', '王五', '王五', '440101199203033456', '中华保险', '家庭财产保险综合险', '财产险', 520.00, 500000.00, 'active', '2024-02-01 00:00:00', '2025-01-31 23:59:59'),
('POL20240025', '赵六', '赵六', '510101198808084567', '泰康保险', '百万医疗保险', '医疗险', 1200.00, 2000000.00, 'active', '2024-04-01 00:00:00', '2025-03-31 23:59:59'),
('POL20240030', '赵六', '赵七', '510101199009095678', '泰康保险', '少儿重疾险', '健康险', 4500.00, 600000.00, 'active', '2024-05-01 00:00:00', '2029-04-30 23:59:59'),
('POL20240035', '孙八', '孙八', '330101199110106789', '平安保险', '意外伤害保险B款', '意外险', 350.00, 200000.00, 'active', '2024-06-01 00:00:00', '2025-05-31 23:59:59'),
('POL20240040', '孙八', '孙八', '330101199110106789', '中国人寿', '养老保险年金险', '寿险', 15000.00, 2000000.00, 'active', '2024-07-01 00:00:00', '2050-06-30 23:59:59'),
('POL20230045', '周九', '周九', '320101198712127890', '太平洋保险', '住院医疗保险', '医疗险', 800.00, 500000.00, 'expired', '2022-01-01 00:00:00', '2023-12-31 23:59:59'),
('POL20240050', '周九', '周九', '320101198712127890', '新华保险', '定期寿险', '寿险', 2800.00, 1000000.00, 'active', '2024-08-01 00:00:00', '2034-07-31 23:59:59'),
('POL20240055', '吴十', '吴十', '110102199502028901', '平安保险', '综合意外险尊享版', '意外险', 480.00, 300000.00, 'active', '2024-09-01 00:00:00', '2025-08-31 23:59:59'),
('POL20240060', '吴十', '吴十一', '110102199603039012', '平安保险', '少儿住院医疗保险', '医疗险', 650.00, 1000000.00, 'active', '2024-10-01 00:00:00', '2025-09-30 23:59:59'),
('POL20230065', '郑十一', '郑十一', '440102198304040123', '友邦保险', '高端医疗保险', '健康险', 18000.00, 5000000.00, 'cancelled', '2022-06-01 00:00:00', '2024-05-31 23:59:59'),
('POL20240070', '郑十一', '郑十一', '440102198304040123', '友邦保险', '重大疾病保险旗舰版', '健康险', 9500.00, 1500000.00, 'active', '2024-11-01 00:00:00', '2029-10-31 23:59:59');
