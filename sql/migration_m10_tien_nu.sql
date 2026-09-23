-- M10: Cai trang Tien Nu
-- Item 2050. Part duoc chon tu dong sau part cuoi cung de tranh de trang bi khac.
-- Anh 17550..17578 da duoc dat trong data/icon/x1..x4.
-- Chay tren VPS:
--   mysql --protocol=socket -u root nro_tien_ma < sql/migration_m10_tien_nu.sql

SET @tien_nu_part_base := COALESCE(
  (SELECT id FROM `part` WHERE `TYPE` = 0 AND `DATA` LIKE '%17550%' LIMIT 1),
  (SELECT COALESCE(MAX(id), -1) + 1 FROM `part`)
);

INSERT INTO `part` (`id`, `TYPE`, `DATA`) VALUES
  (@tien_nu_part_base, 0, '[[17550,-7,-27],[17551,-8,-24],[2955,0,0]]'),
  (@tien_nu_part_base + 1, 1, '[[17552,-12,-19],[17553,-12,-18],[17554,-11,-16],[2955,0,0],[2955,0,0],[2955,0,0],[2955,0,0],[17555,-10,-17],[17556,-3,-14],[17558,-14,-12],[17559,-3,-13],[17560,-6,-13],[17561,-6,-14],[17562,-7,-16],[17563,-9,-18],[17564,-15,-15],[2955,0,0]]'),
  (@tien_nu_part_base + 2, 2, '[[17565,-5,-6],[17566,-5,-17],[17567,-10,-9],[17568,-6,-20],[17569,-14,-22],[17570,-9,-22],[17571,-12,-21],[17572,-11,-11],[17573,-11,-10],[17574,-4,-12],[17575,-2,-4],[17576,-7,-6],[17577,-6,-8],[2955,0,0]]')
ON DUPLICATE KEY UPDATE `TYPE` = VALUES(`TYPE`), `DATA` = VALUES(`DATA`);

INSERT INTO `item_template`
  (`id`, `TYPE`, `gender`, `NAME`, `description`, `level`, `icon_id`, `part`, `is_up_to_up`,
   `power_require`, `gold`, `gem`, `head`, `body`, `leg`, `is_up_to_up_over_99`, `can_trade`, `comment`)
VALUES
  (2050, 5, 3, 'Tiên Nữ', 'Cải trang Tiên Nữ', 1, 17578, -1, 0,
   0, 0, 0, @tien_nu_part_base, @tien_nu_part_base + 1, @tien_nu_part_base + 2, 0, 1, '')
ON DUPLICATE KEY UPDATE
  `TYPE` = VALUES(`TYPE`), `gender` = VALUES(`gender`), `NAME` = VALUES(`NAME`),
  `description` = VALUES(`description`), `icon_id` = VALUES(`icon_id`),
  `head` = VALUES(`head`), `body` = VALUES(`body`), `leg` = VALUES(`leg`),
  `can_trade` = VALUES(`can_trade`);

SELECT id, NAME, icon_id, head, body, leg
FROM item_template
WHERE id = 2050;
