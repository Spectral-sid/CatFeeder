-- phpMyAdmin SQL Dump
-- version 5.2.0
-- https://www.phpmyadmin.net/
--
-- Хост: 127.0.0.1
-- Время создания: Фев 23 2026 г., 06:36
-- Версия сервера: 10.5.16-MariaDB
-- Версия PHP: 7.4.30

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- База данных: `cat_feeder`
--

-- --------------------------------------------------------

--
-- Структура таблицы `appetite_log`
--

CREATE TABLE `appetite_log` (
  `id` int(11) NOT NULL,
  `feeding_id` int(11) NOT NULL,
  `appetite_level` enum('poor','fair','good','excellent') NOT NULL,
  `notes` varchar(255) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------------

--
-- Структура таблицы `cat_breeds`
--

CREATE TABLE `cat_breeds` (
  `id` int(11) NOT NULL,
  `name` varchar(100) NOT NULL,
  `description` text DEFAULT NULL,
  `avg_weight_male` decimal(5,2) DEFAULT NULL COMMENT 'Средний вес котов (кг)',
  `avg_weight_female` decimal(5,2) DEFAULT NULL COMMENT 'Средний вес кошек (кг)',
  `activity_level` enum('low','medium','high') DEFAULT 'medium',
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------------

--
-- Структура таблицы `feeding_history`
--

CREATE TABLE `feeding_history` (
  `id` int(11) NOT NULL,
  `pet_id` int(11) NOT NULL,
  `food_id` int(11) NOT NULL,
  `feeding_date` date NOT NULL,
  `feeding_time` time NOT NULL,
  `amount_grams` decimal(8,2) NOT NULL COMMENT 'Количество в граммах',
  `calories` decimal(8,2) DEFAULT NULL COMMENT 'Рассчитанные калории',
  `bowl_location` varchar(50) DEFAULT NULL COMMENT 'Место кормления (кухня, прихожая и т.д.)',
  `was_finished` tinyint(1) DEFAULT 1 COMMENT 'Была ли порция съедена полностью',
  `notes` varchar(255) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `user_id` int(11) DEFAULT NULL COMMENT 'Кто кормил'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------------

--
-- Структура таблицы `foods`
--

CREATE TABLE `foods` (
  `id` int(11) NOT NULL,
  `barcode` varchar(50) NOT NULL,
  `name` varchar(150) NOT NULL,
  `manufacturer_id` int(11) NOT NULL,
  `food_type_id` int(11) NOT NULL,
  `flavor_id` int(11) DEFAULT NULL,
  `weight_grams` decimal(10,2) DEFAULT NULL COMMENT 'Вес упаковки в граммах',
  `protein_percent` decimal(5,2) DEFAULT NULL COMMENT 'Белок, %',
  `fat_percent` decimal(5,2) DEFAULT NULL COMMENT 'Жиры, %',
  `fiber_percent` decimal(5,2) DEFAULT NULL COMMENT 'Клетчатка, %',
  `moisture_percent` decimal(5,2) DEFAULT NULL COMMENT 'Влажность, %',
  `ash_percent` decimal(5,2) DEFAULT NULL COMMENT 'Зола, %',
  `calories` decimal(8,2) DEFAULT NULL COMMENT 'Ккал на 100г',
  `ingredients` text DEFAULT NULL COMMENT 'Состав',
  `feeding_guide` text DEFAULT NULL COMMENT 'Рекомендации по кормлению',
  `min_age_months` int(11) DEFAULT NULL COMMENT 'Минимальный возраст (месяцев)',
  `max_age_months` int(11) DEFAULT NULL COMMENT 'Максимальный возраст (месяцев)',
  `is_for_kittens` tinyint(1) DEFAULT 0,
  `is_for_adults` tinyint(1) DEFAULT 1,
  `is_for_seniors` tinyint(1) DEFAULT 0,
  `is_for_sterilized` tinyint(1) DEFAULT 0,
  `is_hypoallergenic` tinyint(1) DEFAULT 0,
  `is_grain_free` tinyint(1) DEFAULT 0,
  `photo_path` varchar(500) DEFAULT NULL COMMENT 'Путь к изображению на сервере',
  `thumbnail_path` varchar(500) DEFAULT NULL COMMENT 'Путь к уменьшенному изображению',
  `is_active` tinyint(1) DEFAULT 1,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------------

--
-- Структура таблицы `food_flavors`
--

CREATE TABLE `food_flavors` (
  `id` int(11) NOT NULL,
  `name` varchar(50) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------------

--
-- Структура таблицы `food_types`
--

CREATE TABLE `food_types` (
  `id` int(11) NOT NULL,
  `name` varchar(50) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------------

--
-- Структура таблицы `manufacturers`
--

CREATE TABLE `manufacturers` (
  `id` int(11) NOT NULL,
  `name` varchar(100) NOT NULL,
  `country` varchar(50) DEFAULT NULL,
  `website` varchar(255) DEFAULT NULL,
  `contact_email` varchar(100) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------------

--
-- Структура таблицы `pets`
--

CREATE TABLE `pets` (
  `id` int(11) NOT NULL,
  `name` varchar(100) NOT NULL,
  `breed_id` int(11) DEFAULT NULL,
  `gender` enum('male','female') NOT NULL,
  `birth_date` date DEFAULT NULL COMMENT 'Дата рождения',
  `color` varchar(50) DEFAULT NULL,
  `microchip_number` varchar(50) DEFAULT NULL,
  `sterilization_date` date DEFAULT NULL,
  `current_weight` decimal(5,2) DEFAULT NULL COMMENT 'Текущий вес в кг',
  `target_weight` decimal(5,2) DEFAULT NULL COMMENT 'Целевой вес в кг',
  `daily_calorie_needs` int(11) DEFAULT NULL COMMENT 'Суточная потребность в калориях',
  `is_active` tinyint(1) DEFAULT 1,
  `notes` text DEFAULT NULL,
  `profile_photo_path` varchar(500) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------------

--
-- Структура таблицы `reminders`
--

CREATE TABLE `reminders` (
  `id` int(11) NOT NULL,
  `pet_id` int(11) DEFAULT NULL,
  `reminder_type` enum('feeding','weight','vet','medication','other') NOT NULL,
  `title` varchar(100) NOT NULL,
  `description` text DEFAULT NULL,
  `reminder_date` date NOT NULL,
  `reminder_time` time DEFAULT NULL,
  `is_recurring` tinyint(1) DEFAULT 0,
  `recurrence_pattern` enum('daily','weekly','monthly','yearly') DEFAULT NULL,
  `is_completed` tinyint(1) DEFAULT 0,
  `completed_at` timestamp NULL DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `user_id` int(11) NOT NULL COMMENT 'Кто добавил напоминание'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------------

--
-- Структура таблицы `users`
--

CREATE TABLE `users` (
  `id` int(11) NOT NULL,
  `username` varchar(50) NOT NULL,
  `email` varchar(100) NOT NULL,
  `password_hash` varchar(255) NOT NULL,
  `full_name` varchar(100) DEFAULT NULL,
  `is_active` tinyint(1) DEFAULT 1,
  `last_login` timestamp NULL DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------------

--
-- Структура таблицы `user_pets`
--

CREATE TABLE `user_pets` (
  `user_id` int(11) NOT NULL,
  `pet_id` int(11) NOT NULL,
  `role` enum('owner','caretaker','viewer') DEFAULT 'owner',
  `added_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------------

--
-- Структура таблицы `vet_visits`
--

CREATE TABLE `vet_visits` (
  `id` int(11) NOT NULL,
  `pet_id` int(11) NOT NULL,
  `visit_date` date NOT NULL,
  `reason` varchar(255) NOT NULL,
  `diagnosis` text DEFAULT NULL,
  `treatment` text DEFAULT NULL,
  `next_visit_date` date DEFAULT NULL,
  `vet_clinic` varchar(150) DEFAULT NULL,
  `vet_name` varchar(100) DEFAULT NULL,
  `cost` decimal(10,2) DEFAULT NULL,
  `documents_path` varchar(500) DEFAULT NULL COMMENT 'Путь к сканам документов',
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `user_id` int(11) NOT NULL COMMENT 'Кто носил питомца в клинику'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------------

--
-- Дублирующая структура для представления `v_feeding_details`
-- (См. Ниже фактическое представление)
--
CREATE TABLE `v_feeding_details` (
`id` int(11)
,`feeding_date` date
,`feeding_time` time
,`pet_name` varchar(100)
,`food_name` varchar(150)
,`manufacturer` varchar(100)
,`food_type` varchar(50)
,`flavor` varchar(50)
,`amount_grams` decimal(8,2)
,`calories` decimal(8,2)
,`calculated_calories` decimal(20,8)
,`notes` varchar(255)
,`created_at` timestamp
);

-- --------------------------------------------------------

--
-- Дублирующая структура для представления `v_pet_statistics`
-- (См. Ниже фактическое представление)
--
CREATE TABLE `v_pet_statistics` (
`id` int(11)
,`name` varchar(100)
,`current_weight` decimal(5,2)
,`target_weight` decimal(5,2)
,`breed` varchar(100)
,`previous_weight` decimal(5,2)
,`total_feedings` bigint(21)
,`total_food_grams` decimal(30,2)
,`avg_feeding_amount` decimal(12,6)
,`max_weight` decimal(5,2)
,`min_weight` decimal(5,2)
,`vet_visits_count` bigint(21)
);

-- --------------------------------------------------------

--
-- Структура таблицы `weight_history`
--

CREATE TABLE `weight_history` (
  `id` int(11) NOT NULL,
  `pet_id` int(11) NOT NULL,
  `weight` decimal(5,2) NOT NULL COMMENT 'Вес в кг',
  `measurement_date` date NOT NULL,
  `measurement_time` time DEFAULT '12:00:00',
  `notes` varchar(255) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `user_id` int(11) NOT NULL COMMENT 'Кто взвешивал'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Триггеры `weight_history`
--
DELIMITER $$
CREATE TRIGGER `after_weight_insert` AFTER INSERT ON `weight_history` FOR EACH ROW BEGIN
    UPDATE pets 
    SET current_weight = NEW.weight,
        updated_at = CURRENT_TIMESTAMP
    WHERE id = NEW.pet_id;
END
$$
DELIMITER ;
DELIMITER $$
CREATE TRIGGER `after_weight_update` AFTER UPDATE ON `weight_history` FOR EACH ROW BEGIN
    -- Обновляем только если это последняя запись по дате
    IF NOT EXISTS (
        SELECT 1 FROM weight_history 
        WHERE pet_id = NEW.pet_id 
        AND measurement_date > NEW.measurement_date
        LIMIT 1
    ) THEN
        UPDATE pets 
        SET current_weight = NEW.weight,
            updated_at = CURRENT_TIMESTAMP
        WHERE id = NEW.pet_id;
    END IF;
END
$$
DELIMITER ;

-- --------------------------------------------------------

--
-- Структура для представления `v_feeding_details`
--
DROP TABLE IF EXISTS `v_feeding_details`;

CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY DEFINER VIEW `v_feeding_details`  AS SELECT `fh`.`id` AS `id`, `fh`.`feeding_date` AS `feeding_date`, `fh`.`feeding_time` AS `feeding_time`, `p`.`name` AS `pet_name`, `f`.`name` AS `food_name`, `m`.`name` AS `manufacturer`, `ft`.`name` AS `food_type`, `ff`.`name` AS `flavor`, `fh`.`amount_grams` AS `amount_grams`, `fh`.`calories` AS `calories`, `fh`.`amount_grams`* `f`.`calories` / 100 AS `calculated_calories`, `fh`.`notes` AS `notes`, `fh`.`created_at` AS `created_at` FROM (((((`feeding_history` `fh` join `pets` `p` on(`fh`.`pet_id` = `p`.`id`)) join `foods` `f` on(`fh`.`food_id` = `f`.`id`)) join `manufacturers` `m` on(`f`.`manufacturer_id` = `m`.`id`)) join `food_types` `ft` on(`f`.`food_type_id` = `ft`.`id`)) left join `food_flavors` `ff` on(`f`.`flavor_id` = `ff`.`id`)) ORDER BY `fh`.`feeding_date` DESC, `fh`.`feeding_time` AS `DESCdesc` ASC  ;

-- --------------------------------------------------------

--
-- Структура для представления `v_pet_statistics`
--
DROP TABLE IF EXISTS `v_pet_statistics`;

CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY DEFINER VIEW `v_pet_statistics`  AS SELECT `p`.`id` AS `id`, `p`.`name` AS `name`, `p`.`current_weight` AS `current_weight`, `p`.`target_weight` AS `target_weight`, `cb`.`name` AS `breed`, (select `weight_history`.`weight` from `weight_history` where `weight_history`.`pet_id` = `p`.`id` order by `weight_history`.`measurement_date` desc limit 1,1) AS `previous_weight`, (select count(0) from `feeding_history` where `feeding_history`.`pet_id` = `p`.`id`) AS `total_feedings`, (select sum(`feeding_history`.`amount_grams`) from `feeding_history` where `feeding_history`.`pet_id` = `p`.`id`) AS `total_food_grams`, (select avg(`feeding_history`.`amount_grams`) from `feeding_history` where `feeding_history`.`pet_id` = `p`.`id`) AS `avg_feeding_amount`, (select max(`weight_history`.`weight`) from `weight_history` where `weight_history`.`pet_id` = `p`.`id`) AS `max_weight`, (select min(`weight_history`.`weight`) from `weight_history` where `weight_history`.`pet_id` = `p`.`id`) AS `min_weight`, (select count(0) from `vet_visits` where `vet_visits`.`pet_id` = `p`.`id`) AS `vet_visits_count` FROM (`pets` `p` left join `cat_breeds` `cb` on(`p`.`breed_id` = `cb`.`id`)) WHERE `p`.`is_active` = 11  ;

--
-- Индексы сохранённых таблиц
--

--
-- Индексы таблицы `appetite_log`
--
ALTER TABLE `appetite_log`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_appetite_feeding` (`feeding_id`);

--
-- Индексы таблицы `cat_breeds`
--
ALTER TABLE `cat_breeds`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `idx_breed_name` (`name`);

--
-- Индексы таблицы `feeding_history`
--
ALTER TABLE `feeding_history`
  ADD PRIMARY KEY (`id`),
  ADD KEY `user_id` (`user_id`),
  ADD KEY `idx_feeding_pet` (`pet_id`),
  ADD KEY `idx_feeding_food` (`food_id`),
  ADD KEY `idx_feeding_date` (`feeding_date`),
  ADD KEY `idx_feeding_datetime` (`feeding_date`,`feeding_time`);

--
-- Индексы таблицы `foods`
--
ALTER TABLE `foods`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `barcode` (`barcode`),
  ADD KEY `flavor_id` (`flavor_id`),
  ADD KEY `idx_food_barcode` (`barcode`),
  ADD KEY `idx_food_name` (`name`),
  ADD KEY `idx_food_manufacturer` (`manufacturer_id`),
  ADD KEY `idx_food_type` (`food_type_id`);

--
-- Индексы таблицы `food_flavors`
--
ALTER TABLE `food_flavors`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `idx_flavor_name` (`name`);

--
-- Индексы таблицы `food_types`
--
ALTER TABLE `food_types`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `idx_food_type_name` (`name`);

--
-- Индексы таблицы `manufacturers`
--
ALTER TABLE `manufacturers`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_manufacturer_name` (`name`);

--
-- Индексы таблицы `pets`
--
ALTER TABLE `pets`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `microchip_number` (`microchip_number`),
  ADD KEY `idx_pet_name` (`name`),
  ADD KEY `idx_pet_breed` (`breed_id`),
  ADD KEY `idx_pet_active` (`is_active`);

--
-- Индексы таблицы `reminders`
--
ALTER TABLE `reminders`
  ADD PRIMARY KEY (`id`),
  ADD KEY `user_id` (`user_id`),
  ADD KEY `idx_reminder_pet` (`pet_id`),
  ADD KEY `idx_reminder_date` (`reminder_date`),
  ADD KEY `idx_reminder_completed` (`is_completed`);

--
-- Индексы таблицы `users`
--
ALTER TABLE `users`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `username` (`username`),
  ADD UNIQUE KEY `email` (`email`),
  ADD KEY `idx_user_username` (`username`),
  ADD KEY `idx_user_email` (`email`);

--
-- Индексы таблицы `user_pets`
--
ALTER TABLE `user_pets`
  ADD PRIMARY KEY (`user_id`,`pet_id`),
  ADD KEY `idx_user_pets_user` (`user_id`),
  ADD KEY `idx_user_pets_pet` (`pet_id`);

--
-- Индексы таблицы `vet_visits`
--
ALTER TABLE `vet_visits`
  ADD PRIMARY KEY (`id`),
  ADD KEY `user_id` (`user_id`),
  ADD KEY `idx_vet_pet` (`pet_id`),
  ADD KEY `idx_vet_date` (`visit_date`);

--
-- Индексы таблицы `weight_history`
--
ALTER TABLE `weight_history`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `unique_pet_date` (`pet_id`,`measurement_date`),
  ADD KEY `user_id` (`user_id`),
  ADD KEY `idx_weight_pet` (`pet_id`),
  ADD KEY `idx_weight_date` (`measurement_date`);

--
-- AUTO_INCREMENT для сохранённых таблиц
--

--
-- AUTO_INCREMENT для таблицы `appetite_log`
--
ALTER TABLE `appetite_log`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT для таблицы `cat_breeds`
--
ALTER TABLE `cat_breeds`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT для таблицы `feeding_history`
--
ALTER TABLE `feeding_history`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT для таблицы `foods`
--
ALTER TABLE `foods`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT для таблицы `food_flavors`
--
ALTER TABLE `food_flavors`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT для таблицы `food_types`
--
ALTER TABLE `food_types`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT для таблицы `manufacturers`
--
ALTER TABLE `manufacturers`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT для таблицы `pets`
--
ALTER TABLE `pets`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT для таблицы `reminders`
--
ALTER TABLE `reminders`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT для таблицы `users`
--
ALTER TABLE `users`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT для таблицы `vet_visits`
--
ALTER TABLE `vet_visits`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT для таблицы `weight_history`
--
ALTER TABLE `weight_history`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- Ограничения внешнего ключа сохраненных таблиц
--

--
-- Ограничения внешнего ключа таблицы `appetite_log`
--
ALTER TABLE `appetite_log`
  ADD CONSTRAINT `appetite_log_ibfk_1` FOREIGN KEY (`feeding_id`) REFERENCES `feeding_history` (`id`) ON DELETE CASCADE;

--
-- Ограничения внешнего ключа таблицы `feeding_history`
--
ALTER TABLE `feeding_history`
  ADD CONSTRAINT `feeding_history_ibfk_1` FOREIGN KEY (`pet_id`) REFERENCES `pets` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `feeding_history_ibfk_2` FOREIGN KEY (`food_id`) REFERENCES `foods` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `feeding_history_ibfk_3` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Ограничения внешнего ключа таблицы `foods`
--
ALTER TABLE `foods`
  ADD CONSTRAINT `foods_ibfk_1` FOREIGN KEY (`manufacturer_id`) REFERENCES `manufacturers` (`id`),
  ADD CONSTRAINT `foods_ibfk_2` FOREIGN KEY (`food_type_id`) REFERENCES `food_types` (`id`),
  ADD CONSTRAINT `foods_ibfk_3` FOREIGN KEY (`flavor_id`) REFERENCES `food_flavors` (`id`) ON DELETE SET NULL;

--
-- Ограничения внешнего ключа таблицы `pets`
--
ALTER TABLE `pets`
  ADD CONSTRAINT `pets_ibfk_1` FOREIGN KEY (`breed_id`) REFERENCES `cat_breeds` (`id`) ON DELETE SET NULL;

--
-- Ограничения внешнего ключа таблицы `reminders`
--
ALTER TABLE `reminders`
  ADD CONSTRAINT `reminders_ibfk_1` FOREIGN KEY (`pet_id`) REFERENCES `pets` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `reminders_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Ограничения внешнего ключа таблицы `user_pets`
--
ALTER TABLE `user_pets`
  ADD CONSTRAINT `user_pets_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `user_pets_ibfk_2` FOREIGN KEY (`pet_id`) REFERENCES `pets` (`id`) ON DELETE CASCADE;

--
-- Ограничения внешнего ключа таблицы `vet_visits`
--
ALTER TABLE `vet_visits`
  ADD CONSTRAINT `vet_visits_ibfk_1` FOREIGN KEY (`pet_id`) REFERENCES `pets` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `vet_visits_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Ограничения внешнего ключа таблицы `weight_history`
--
ALTER TABLE `weight_history`
  ADD CONSTRAINT `weight_history_ibfk_1` FOREIGN KEY (`pet_id`) REFERENCES `pets` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `weight_history_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
