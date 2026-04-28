-- ============================================================================
-- 小糯智肤 (nuonuo-agent) 极简成分安全种子数据
-- 用途: 支撑 AC 自动机快轨、第1层单体精准查表 (P0) 与第2层大类机理推断 (P1) 验证
-- 约束: ingredient_a < ingredient_b、category_a < category_b 按字典序存储
-- ============================================================================

-- 1. 成分标准字典表 (10 个高频代表性成分)
INSERT INTO `ingredient_dict` (
    `id`, `inci_name_cn`, `inci_name_en`, `alias_names`, `category`,
    `ph_min`, `ph_max`, `irritancy_level`, `is_preservative`, `is_sensitizer`
) VALUES
(100001, '水杨酸', 'Salicylic Acid', '["BHA", "2-羟基苯甲酸"]', 'HIGH_ACID', 3.0, 4.0, 2, 0, 0),
(100002, '壬二酸', 'Azelaic Acid', '["杜鹃花酸"]', 'HIGH_ACID', 4.0, 5.0, 1, 0, 0),
(100003, '果酸', 'Glycolic Acid', '["AHA", "乙醇酸", "甘醇酸"]', 'HIGH_ACID', 3.0, 4.0, 2, 0, 0),
(100004, '视黄醇', 'Retinol', '["A醇", "维生素A醇"]', 'RETINOIDS', 5.5, 6.5, 2, 0, 0),
(100005, '羟基频哪酮视黄酸酯', 'Hydroxypinacolone Retinoate', '["HPR", "超A"]', 'RETINOIDS', 5.0, 6.5, 1, 0, 0),
(100006, '蓝铜胜肽', 'Copper Tripeptide-1', '["三肽-1铜", "GHK-Cu"]', 'METAL_PEPTIDE', 5.5, 7.0, 0, 0, 0),
(100007, '抗坏血酸', 'Ascorbic Acid', '["原型VC", "左旋维C", "L-抗坏血酸"]', 'PURE_VC', 2.5, 3.5, 2, 0, 0),
(100008, '烟酰胺', 'Niacinamide', '["维生素B3", "VB3"]', 'NIACINAMIDE', 5.0, 7.0, 1, 0, 0),
(100009, '泛醇', 'Panthenol', '["维生素B5", "B5", "D-泛醇"]', 'GENTLE_HYDRATOR', 4.5, 7.5, 0, 0, 0),
(100010, '神经酰胺NP', 'Ceramide NP', '["神经酰胺3", "神经酰胺"]', 'GENTLE_HYDRATOR', 4.5, 7.0, 0, 0, 0);

-- 2. 成分配伍禁忌规则表 (3 条单体 P0 规则 + 3 条大类 P1 规则)
-- 注: 故意不录入 "壬二酸 + 蓝铜胜肽" 单体规则，用于验证第2层大类理化推断 (HIGH_ACID + METAL_PEPTIDE)
INSERT INTO `ingredient_conflict_rule` (
    `id`, `rule_level`, `ingredient_a`, `ingredient_b`, `category_a`, `category_b`,
    `conflict_level`, `risk_mechanism`, `handling_advice`
) VALUES
(200001, 'SINGLE', '水杨酸', '视黄醇', NULL, NULL,
 'STRICT_FORBIDDEN',
 '高浓度水杨酸剥脱角质并降低表皮pH，叠加视黄醇会显著放大屏障刺痛、脱皮与泛红风险。',
 '严禁同晚叠加使用；建议早晚分用或隔天交替使用，并搭配B5/神经酰胺修护保湿。'),
(200002, 'SINGLE', '水杨酸', '蓝铜胜肽', NULL, NULL,
 'STRICT_FORBIDDEN',
 '水杨酸的低pH酸性环境会破坏蓝铜胜肽的铜离子络合结构，导致多肽水解失活并析出游离铜离子刺激皮肤。',
 '建议错开时段使用（如早用酸类控油、晚用蓝铜胜肽修护），或间隔至少12小时。'),
(200003, 'SINGLE', '抗坏血酸', '烟酰胺', NULL, NULL,
 'CAUTION',
 '高浓度原型VC需低pH起效，与烟酰胺同用可能引起烟酰胺转化为烟酸，引发部分敏感肌潮红刺痛。',
 '敏感肌建议早C（原型VC）晚B3（烟酰胺）分时使用；耐受肌可先局部耳后测试。'),
(200004, 'CATEGORY', NULL, NULL, 'HIGH_ACID', 'METAL_PEPTIDE',
 'STRICT_FORBIDDEN',
 '强酸类成分的低pH环境会使金属络合多肽发生水解变性并析出游离金属离子，既失效又易引发红斑。',
 '请勿在同一护肤流程中叠加强酸与金属多肽，建议分早晚或隔日使用。'),
(200005, 'CATEGORY', NULL, NULL, 'HIGH_ACID', 'RETINOIDS',
 'STRICT_FORBIDDEN',
 '强酸剥脱角质层叠加A醇类加速表皮更替，双重刺激极易击穿角质屏障引发接触性皮炎。',
 '建议精简流程、隔日交替建立耐受，切勿同晚叠涂。'),
(200006, 'CATEGORY', NULL, NULL, 'NIACINAMIDE', 'PURE_VC',
 'CAUTION',
 '原型维C的强酸性配方与烟酰胺直接混合可能产生烟酸刺激，并影响各自最佳起效pH窗口。',
 '建议采用早间原型VC抗氧化、晚间烟酰胺修护提亮的错峰搭配方案。');
