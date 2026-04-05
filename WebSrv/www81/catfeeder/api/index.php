<?php
// Включаем вывод всех ошибок для отладки
error_reporting(E_ALL);
ini_set('display_errors', 1);
ini_set('log_errors', 1);
ini_set('error_log', '/tmp/catfeeder_php_errors.log');

// CORS заголовки
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS");
header("Access-Control-Max-Age: 3600");
header("Access-Control-Allow-Headers: Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");

// Обработка OPTIONS запроса
if ($_SERVER['REQUEST_METHOD'] == 'OPTIONS') {
    http_response_code(200);
    exit();
}

// Подключение к базе данных
require_once 'config/database.php';
$database = new Database();
$db = $database->getConnection();

// Подключение утилит
require_once 'utils/Response.php';

// Получаем путь запроса
$request_uri = $_SERVER['REQUEST_URI'];
$script_name = $_SERVER['SCRIPT_NAME'];

// Отладочная информация
error_log("=== CATFEEDER API REQUEST ===");
error_log("Request URI: " . $request_uri);
error_log("Script Name: " . $script_name);
error_log("Request Method: " . $_SERVER['REQUEST_METHOD']);

// Убираем базовый путь из URI
$base_path = dirname($script_name);
$path = str_replace($base_path, '', $request_uri);
$path = parse_url($path, PHP_URL_PATH);
$path = trim($path, '/');

error_log("Clean Path: " . $path);

// Разбиваем путь на части
$path_parts = $path ? explode('/', $path) : [];
$endpoint = $path_parts[0] ?? '';
$action = $path_parts[1] ?? '';
$param = $path_parts[2] ?? '';

error_log("Endpoint: " . $endpoint);
error_log("Action: " . $action);
error_log("Param: " . $param);

// ============================================
// МАРШРУТИЗАЦИЯ API
// ============================================

// ===== HEALTH CHECK =====
if ($path === '' || $path === 'health') {
    Response::sendSuccess([
        'status' => 'online',
        'message' => 'API работает нормально',
        'timestamp' => date('Y-m-d H:i:s'),
        'server' => $_SERVER['SERVER_SOFTWARE'] ?? 'Nginx/Apache',
        'php_version' => phpversion(),
        'database' => $db ? 'connected' : 'disconnected'
    ]);
}

// ===== ПИТОМЦЫ (PETS) =====
elseif ($endpoint === 'pets') {
    
    require_once 'models/Pet.php';
    $pet = new Pet($db);
    
    // GET /pets - список всех питомцев
    if ($_SERVER['REQUEST_METHOD'] === 'GET' && $action === '') {
        try {
            $stmt = $pet->getAll();
            $pets = $stmt->fetchAll();
            
            $result = array_map(function($p) {
                return [
                    'id' => (int)$p['id'],
                    'name' => $p['name'],
                    'breed' => $p['breed_name'] ?? 'Не указана',
                    'gender' => $p['gender'] === 'male' ? 'Самец' : 'Самка',
                    'birthDate' => $p['birth_date'],
                    'currentWeight' => $p['current_weight'] ? (float)$p['current_weight'] : null,
                    'targetWeight' => $p['target_weight'] ? (float)$p['target_weight'] : null,
                    'profilePhoto' => $p['profile_photo_path'],
                    'isActive' => (bool)$p['is_active']
                ];
            }, $pets);
            
            Response::sendSuccess($result);
        } catch (Exception $e) {
            Response::sendError('Ошибка при получении питомцев: ' . $e->getMessage(), 500);
        }
    }
    
    // GET /pets/{id} - получить питомца по ID
    elseif ($_SERVER['REQUEST_METHOD'] === 'GET' && is_numeric($action)) {
        try {
            $petData = $pet->getById($action);
            
            if ($petData) {
                $result = [
                    'id' => (int)$petData['id'],
                    'name' => $petData['name'],
                    'breed' => $petData['breed_name'] ?? 'Не указана',
                    'gender' => $petData['gender'] === 'male' ? 'Самец' : 'Самка',
                    'birthDate' => $petData['birth_date'],
                    'currentWeight' => $petData['current_weight'] ? (float)$petData['current_weight'] : null,
                    'targetWeight' => $petData['target_weight'] ? (float)$petData['target_weight'] : null,
                    'profilePhoto' => $petData['profile_photo_path']
                ];
                Response::sendSuccess($result);
            } else {
                Response::sendError('Питомец не найден', 404);
            }
        } catch (Exception $e) {
            Response::sendError('Ошибка при получении питомца: ' . $e->getMessage(), 500);
        }
    }
    
    // POST /pets/weight - запись веса
    elseif ($_SERVER['REQUEST_METHOD'] === 'POST' && $action === 'weight') {
        try {
            $data = json_decode(file_get_contents('php://input'), true);
            
            if (!isset($data['petId']) || !isset($data['weight'])) {
                Response::sendError('Не указаны обязательные параметры: petId, weight');
            }
            
            $petId = $data['petId'];
            $weight = $data['weight'];
            $date = $data['date'] ?? date('Y-m-d');
            $notes = $data['notes'] ?? null;
            
            $id = $pet->addWeight($petId, $weight, $date, $notes);
            
            if ($id) {
                Response::sendSuccess(['id' => $id], 'Вес успешно записан');
            } else {
                Response::sendError('Ошибка при записи веса');
            }
        } catch (Exception $e) {
            Response::sendError('Ошибка при записи веса: ' . $e->getMessage(), 500);
        }
    }
    
    // GET /pets/{id}/weight-history - история веса
    elseif ($_SERVER['REQUEST_METHOD'] === 'GET' && $action === 'weight-history' && is_numeric($param)) {
        try {
            $stmt = $pet->getWeightHistory($param);
            $history = $stmt->fetchAll();
            
            $result = array_map(function($w) {
                return [
                    'id' => (int)$w['id'],
                    'petId' => (int)$w['pet_id'],
                    'weight' => (float)$w['weight'],
                    'measurementDate' => $w['measurement_date'],
                    'notes' => $w['notes'],
                    'createdAt' => $w['created_at']
                ];
            }, $history);
            
            Response::sendSuccess($result);
        } catch (Exception $e) {
            Response::sendError('Ошибка при получении истории веса: ' . $e->getMessage(), 500);
        }
    }
    
    // GET /pets/{id}/stats - статистика по питомцу
    elseif ($_SERVER['REQUEST_METHOD'] === 'GET' && $action === 'stats' && is_numeric($param)) {
        try {
            $startDate = $_GET['startDate'] ?? null;
            $endDate = $_GET['endDate'] ?? null;
            
            $sql = "SELECT 
                    COUNT(*) as feeding_count,
                    SUM(amount_grams) as total_food,
                    AVG(amount_grams) as avg_amount,
                    MIN(feeding_date) as first_date,
                    MAX(feeding_date) as last_date
                    FROM feeding_history 
                    WHERE pet_id = :pet_id";
            
            $params = [':pet_id' => $param];
            
            if ($startDate) {
                $sql .= " AND feeding_date >= :start_date";
                $params[':start_date'] = $startDate;
            }
            
            if ($endDate) {
                $sql .= " AND feeding_date <= :end_date";
                $params[':end_date'] = $endDate;
            }
            
            $stmt = $db->prepare($sql);
            $stmt->execute($params);
            $stats = $stmt->fetch();
            
            Response::sendSuccess([
                'feedingCount' => (int)$stats['feeding_count'],
                'totalFood' => (float)$stats['total_food'],
                'avgAmount' => (float)$stats['avg_amount'],
                'firstDate' => $stats['first_date'],
                'lastDate' => $stats['last_date']
            ]);
        } catch (Exception $e) {
            Response::sendError('Ошибка при получении статистики: ' . $e->getMessage(), 500);
        }
    }
    
    else {
        Response::sendError('Метод не найден', 404);
    }
}

// ===== КОРМА (FOODS) =====
elseif ($endpoint === 'foods') {
    
    require_once 'models/Food.php';
    $food = new Food($db);
    
    // GET /foods/barcode/{barcode} - поиск по штрихкоду
    if ($_SERVER['REQUEST_METHOD'] === 'GET' && $action === 'barcode' && $param) {
        try {
            $foodData = $food->getByBarcode($param);
            
            if ($foodData) {
                $result = [
                    'id' => (int)$foodData['id'],
                    'barcode' => $foodData['barcode'],
                    'name' => $foodData['name'],
                    'manufacturer' => $foodData['manufacturer_name'],
                    'type' => $foodData['type_name'],
                    'flavor' => $foodData['flavor_name'],
                    'weight' => $foodData['weight_grams'] ? (float)$foodData['weight_grams'] : null,
                    'protein' => $foodData['protein_percent'] ? (float)$foodData['protein_percent'] : null,
                    'fat' => $foodData['fat_percent'] ? (float)$foodData['fat_percent'] : null,
                    'calories' => $foodData['calories'] ? (float)$foodData['calories'] : null,
                    'photo' => $foodData['photo_path']
                ];
                Response::sendSuccess($result);
            } else {
                Response::sendSuccess(null, 'Корм не найден в базе');
            }
        } catch (Exception $e) {
            Response::sendError('Ошибка при поиске корма: ' . $e->getMessage(), 500);
        }
    }
    
    // POST /foods - создание нового корма
    elseif ($_SERVER['REQUEST_METHOD'] === 'POST' && $action === '') {
        try {
            $data = json_decode(file_get_contents('php://input'), true);
            
            if (!isset($data['barcode']) || !isset($data['name'])) {
                Response::sendError('Не указаны обязательные параметры: barcode, name');
            }
            
            $success = $food->create($data);
            
            if ($success) {
                $id = $db->lastInsertId();
                Response::sendSuccess(['id' => $id], 'Корм успешно добавлен');
            } else {
                Response::sendError('Ошибка при добавлении корма');
            }
        } catch (Exception $e) {
            Response::sendError('Ошибка при добавлении корма: ' . $e->getMessage(), 500);
        }
    }
	
	// PUT /foods/{id} - обновление корма
elseif ($_SERVER['REQUEST_METHOD'] === 'PUT' && is_numeric($action)) {
    try {
        $foodId = (int)$action;
        $data = json_decode(file_get_contents('php://input'), true);
        
        if (!isset($data['name'])) {
            Response::sendError('Не указано название корма');
        }
        
        $sql = "UPDATE foods SET 
                name = :name,
                manufacturer_id = :manufacturer_id,
                food_type_id = :food_type_id,
                flavor_id = :flavor_id,
                weight_grams = :weight_grams,
                calories = :calories,
                protein_percent = :protein_percent,
                fat_percent = :fat_percent,
                updated_at = NOW()
                WHERE id = :id";
        
        $stmt = $db->prepare($sql);
        $stmt->execute([
            ':id' => $foodId,
            ':name' => $data['name'],
            ':manufacturer_id' => $data['manufacturerId'],
            ':food_type_id' => $data['foodTypeId'],
            ':flavor_id' => $data['flavorId'] ?? null,
            ':weight_grams' => $data['weight'] ?? null,
            ':calories' => $data['calories'] ?? null,
            ':protein_percent' => $data['protein'] ?? null,
            ':fat_percent' => $data['fat'] ?? null
        ]);
        
        Response::sendSuccess(null, 'Корм успешно обновлен');
    } catch (Exception $e) {
        Response::sendError('Ошибка: ' . $e->getMessage(), 500);
    }
}
    
    // GET /foods - список всех кормов
    elseif ($_SERVER['REQUEST_METHOD'] === 'GET' && $action === '') {
        try {
            $sql = "SELECT f.*, m.name as manufacturer_name, ft.name as type_name 
                    FROM foods f
                    JOIN manufacturers m ON f.manufacturer_id = m.id
                    JOIN food_types ft ON f.food_type_id = ft.id
                    WHERE f.is_active = TRUE
                    ORDER BY f.name
                    LIMIT 100";
            
            $stmt = $db->query($sql);
            $foods = $stmt->fetchAll();
            
            $result = array_map(function($f) {
                return [
                    'id' => (int)$f['id'],
                    'barcode' => $f['barcode'],
                    'name' => $f['name'],
                    'manufacturer' => $f['manufacturer_name'],
                    'type' => $f['type_name'],
                    'calories' => $f['calories'] ? (float)$f['calories'] : null
                ];
            }, $foods);
            
            Response::sendSuccess($result);
        } catch (Exception $e) {
            Response::sendError('Ошибка при получении кормов: ' . $e->getMessage(), 500);
        }
    }
    
    else {
        Response::sendError('Метод не найден', 404);
    }
}

// ===== КОРМЛЕНИЯ (FEEDING) =====
elseif ($endpoint === 'feeding') {
    
    require_once 'models/Feeding.php';
    $feeding = new Feeding($db);
    
    // POST /feeding - запись кормления
    if ($_SERVER['REQUEST_METHOD'] === 'POST' && $action === '') {
        try {
            $data = json_decode(file_get_contents('php://input'), true);
            
            if (!isset($data['petId']) || !isset($data['amount'])) {
                Response::sendError('Не указаны обязательные параметры: petId, amount');
            }
            
            if (!isset($data['foodId']) && !isset($data['barcode'])) {
                Response::sendError('Не указан корм (foodId или barcode)');
            }
            
            $dbData = [
                'pet_id' => $data['petId'],
                'amount' => $data['amount'],
                'barcode' => $data['barcode'] ?? null,
                'food_id' => $data['foodId'] ?? null,
                'food_name' => $data['foodName'] ?? null,
                'feeding_date' => $data['feedingDate'] ?? date('Y-m-d'),
                'feeding_time' => $data['feedingTime'] ?? date('H:i:s'),
                'calories' => $data['calories'] ?? null,
                'was_finished' => $data['wasFinished'] ?? 100,
                'notes' => $data['notes'] ?? null
            ];
            
            $id = $feeding->create($dbData);
            
            if ($id) {
                Response::sendSuccess(['id' => $id], 'Кормление успешно записано');
            } else {
                Response::sendError('Ошибка при записи кормления');
            }
        } catch (Exception $e) {
            Response::sendError('Ошибка при записи кормления: ' . $e->getMessage(), 500);
        }
    }
    
    // GET /feeding/history/{petId} - история кормлений для питомца
    elseif ($_SERVER['REQUEST_METHOD'] === 'GET' && $action === 'history' && is_numeric($param)) {
        try {
            $petId = $param;
            $startDate = $_GET['startDate'] ?? null;
            $endDate = $_GET['endDate'] ?? null;
            $limit = $_GET['limit'] ?? 100;
            $offset = $_GET['offset'] ?? 0;
            
            $history = $feeding->getHistory($petId, $startDate, $endDate, $limit, $offset);
            
            $result = array_map(function($item) {
                return [
                    'id' => (int)$item['id'],
                    'date' => $item['feeding_date'],
                    'time' => $item['feeding_time'],
                    'foodName' => $item['food_name'],
                    'barcode' => $item['barcode'],
                    'manufacturer' => $item['manufacturer'] ?? null,
                    'type' => $item['type_name'] ?? null,
                    'flavor' => $item['flavor_name'] ?? null,
                    'amount' => (float)$item['amount_grams'],
                    'wasFinished' => (int)($item['was_finished'] ?? 100),
                    'calories' => $item['calories'] ? (float)$item['calories'] : null,
                    'notes' => $item['notes'],
                    'petId' => (int)$item['pet_id']
                ];
            }, $history);
            
            Response::sendSuccess($result);
        } catch (Exception $e) {
            Response::sendError('Ошибка при получении истории: ' . $e->getMessage(), 500);
        }
    }
    
    // GET /feeding/history - вся история
    elseif ($_SERVER['REQUEST_METHOD'] === 'GET' && $action === 'history' && $param === '') {
        try {
            $startDate = $_GET['startDate'] ?? null;
            $endDate = $_GET['endDate'] ?? null;
            $limit = $_GET['limit'] ?? 100;
            $offset = $_GET['offset'] ?? 0;
            
            $sql = "SELECT fh.*, f.name as food_name, f.barcode, m.name as manufacturer, p.name as pet_name,
                    ft.name as type_name, ff.name as flavor_name
                    FROM feeding_history fh
                    JOIN foods f ON fh.food_id = f.id
                    JOIN food_types ft ON f.food_type_id = ft.id
                    LEFT JOIN food_flavors ff ON f.flavor_id = ff.id
                    JOIN manufacturers m ON f.manufacturer_id = m.id
                    JOIN pets p ON fh.pet_id = p.id
                    WHERE 1=1";
            
            $params = [];
            
            if ($startDate) {
                $sql .= " AND fh.feeding_date >= :startDate";
                $params[':startDate'] = $startDate;
            }
            
            if ($endDate) {
                $sql .= " AND fh.feeding_date <= :endDate";
                $params[':endDate'] = $endDate;
            }
            
            $sql .= " ORDER BY fh.feeding_date DESC, fh.feeding_time DESC LIMIT :limit OFFSET :offset";
            $params[':limit'] = (int)$limit;
            $params[':offset'] = (int)$offset;
            
            $stmt = $db->prepare($sql);
            foreach ($params as $key => $value) {
                if (is_int($value)) {
                    $stmt->bindValue($key, $value, PDO::PARAM_INT);
                } else {
                    $stmt->bindValue($key, $value);
                }
            }
            $stmt->execute();
            
            $history = $stmt->fetchAll();
            
            $result = array_map(function($item) {
                return [
                    'id' => (int)$item['id'],
                    'date' => $item['feeding_date'],
                    'time' => $item['feeding_time'],
                    'foodName' => $item['food_name'],
                    'barcode' => $item['barcode'],
                    'manufacturer' => $item['manufacturer'],
                    'type' => $item['type_name'],
                    'flavor' => $item['flavor_name'],
                    'amount' => (float)$item['amount_grams'],
                    'wasFinished' => (int)($item['was_finished'] ?? 100),
                    'calories' => $item['calories'] ? (float)$item['calories'] : null,
                    'notes' => $item['notes'],
                    'petId' => (int)$item['pet_id'],
                    'petName' => $item['pet_name']
                ];
            }, $history);
            
            Response::sendSuccess($result);
        } catch (Exception $e) {
            Response::sendError('Ошибка при получении истории: ' . $e->getMessage(), 500);
        }
    }
    
    // GET /feeding/{id} - получение конкретного кормления
    elseif ($_SERVER['REQUEST_METHOD'] === 'GET' && is_numeric($action)) {
        try {
            $feedingId = $action;
            
            $sql = "SELECT fh.*, f.name as food_name, f.barcode, m.name as manufacturer, p.name as pet_name,
                    ft.name as type_name, ff.name as flavor_name
                    FROM feeding_history fh
                    JOIN foods f ON fh.food_id = f.id
                    JOIN food_types ft ON f.food_type_id = ft.id
                    LEFT JOIN food_flavors ff ON f.flavor_id = ff.id
                    JOIN manufacturers m ON f.manufacturer_id = m.id
                    JOIN pets p ON fh.pet_id = p.id
                    WHERE fh.id = :id";
            
            $stmt = $db->prepare($sql);
            $stmt->execute([':id' => $feedingId]);
            $item = $stmt->fetch();
            
            if ($item) {
                $result = [
                    'id' => (int)$item['id'],
                    'date' => $item['feeding_date'],
                    'time' => $item['feeding_time'],
                    'foodName' => $item['food_name'],
                    'barcode' => $item['barcode'],
                    'manufacturer' => $item['manufacturer'],
                    'type' => $item['type_name'],
                    'flavor' => $item['flavor_name'],
                    'amount' => (float)$item['amount_grams'],
                    'wasFinished' => (int)($item['was_finished'] ?? 100),
                    'calories' => $item['calories'] ? (float)$item['calories'] : null,
                    'notes' => $item['notes'],
                    'petId' => (int)$item['pet_id'],
                    'petName' => $item['pet_name']
                ];
                Response::sendSuccess($result);
            } else {
                Response::sendError('Кормление не найдено', 404);
            }
        } catch (Exception $e) {
            Response::sendError('Ошибка при получении кормления: ' . $e->getMessage(), 500);
        }
    }
    
    // PUT /feeding/{id}/was-finished - обновить статус съеденного
	// Здесь $action - это ID, $param - это "was-finished"
    elseif ($_SERVER['REQUEST_METHOD'] === 'PUT' && $param  === 'was-finished' && is_numeric($action)) {
        try {
            $feedingId = $action;
            $data = json_decode(file_get_contents('php://input'), true);
            
            if (!isset($data['wasFinished'])) {
                Response::sendError('Не указан параметр wasFinished');
            }
            
            $wasFinished = (int)$data['wasFinished'];
            
            if ($wasFinished < 0 || $wasFinished > 100) {
                Response::sendError('Параметр wasFinished должен быть от 0 до 100');
            }
            
            $success = $feeding->updateWasFinished($feedingId, $wasFinished);
            
            if ($success) {
                Response::sendSuccess(null, 'Статус обновлен');
            } else {
                Response::sendError('Ошибка при обновлении статуса');
            }
        } catch (Exception $e) {
            Response::sendError('Ошибка: ' . $e->getMessage(), 500);
        }
    }
    
    else {
        Response::sendError('Метод не найден', 404);
    }
}

// ===== ПРОИЗВОДИТЕЛИ (MANUFACTURERS) =====
elseif ($endpoint === 'manufacturers') {
    
    if ($_SERVER['REQUEST_METHOD'] === 'GET') {
        try {
            $sql = "SELECT id, name, country FROM manufacturers ORDER BY name";
            $stmt = $db->query($sql);
            $manufacturers = $stmt->fetchAll();
            Response::sendSuccess($manufacturers);
        } catch (Exception $e) {
            Response::sendError('Ошибка при получении производителей: ' . $e->getMessage(), 500);
        }
    }
    elseif ($_SERVER['REQUEST_METHOD'] === 'POST') {
        try {
            $data = json_decode(file_get_contents('php://input'), true);
            
            if (!isset($data['name'])) {
                Response::sendError('Не указано название производителя');
            }
            
            $sql = "INSERT INTO manufacturers (name, country, created_at) VALUES (:name, :country, NOW())";
            $stmt = $db->prepare($sql);
            $stmt->execute([
                ':name' => $data['name'],
                ':country' => $data['country'] ?? null
            ]);
            
            $id = $db->lastInsertId();
            Response::sendSuccess(['id' => $id], 'Производитель добавлен');
        } catch (Exception $e) {
            Response::sendError('Ошибка: ' . $e->getMessage(), 500);
        }
    }
    else {
        Response::sendError('Метод не поддерживается', 405);
    }
}

// ===== ТИПЫ КОРМОВ (FOOD TYPES) =====
elseif ($endpoint === 'food-types') {
    
    if ($_SERVER['REQUEST_METHOD'] === 'GET') {
        try {
            $sql = "SELECT id, name FROM food_types ORDER BY name";
            $stmt = $db->query($sql);
            $types = $stmt->fetchAll();
            Response::sendSuccess($types);
        } catch (Exception $e) {
            Response::sendError('Ошибка при получении типов кормов: ' . $e->getMessage(), 500);
        }
    } else {
        Response::sendError('Метод не поддерживается', 405);
    }
}

// ===== ВКУСЫ КОРМОВ (FLAVORS) =====
elseif ($endpoint === 'flavors') {
    
    if ($_SERVER['REQUEST_METHOD'] === 'GET') {
        try {
            $sql = "SELECT id, name FROM food_flavors ORDER BY name";
            $stmt = $db->query($sql);
            $flavors = $stmt->fetchAll();
            Response::sendSuccess($flavors);
        } catch (Exception $e) {
            Response::sendError('Ошибка при получении вкусов: ' . $e->getMessage(), 500);
        }
    }
    elseif ($_SERVER['REQUEST_METHOD'] === 'POST') {
        try {
            $data = json_decode(file_get_contents('php://input'), true);
            
            if (!isset($data['name'])) {
                Response::sendError('Не указано название вкуса');
            }
            
            $sql = "INSERT INTO food_flavors (name, created_at) VALUES (:name, NOW())";
            $stmt = $db->prepare($sql);
            $stmt->execute([':name' => $data['name']]);
            
            $id = $db->lastInsertId();
            Response::sendSuccess(['id' => $id], 'Вкус добавлен');
        } catch (Exception $e) {
            Response::sendError('Ошибка: ' . $e->getMessage(), 500);
        }
    }
    else {
        Response::sendError('Метод не поддерживается', 405);
    }
}

// ===== ПОРОДЫ (BREEDS) =====
elseif ($endpoint === 'breeds') {
    
    if ($_SERVER['REQUEST_METHOD'] === 'GET') {
        try {
            $sql = "SELECT id, name FROM cat_breeds ORDER BY name";
            $stmt = $db->query($sql);
            $breeds = $stmt->fetchAll();
            Response::sendSuccess($breeds);
        } catch (Exception $e) {
            Response::sendError('Ошибка при получении пород: ' . $e->getMessage(), 500);
        }
    } else {
        Response::sendError('Метод не поддерживается', 405);
    }
}

// ===== СТАТИСТИКА (STATS) =====
elseif ($endpoint === 'stats') {
    
    if ($_SERVER['REQUEST_METHOD'] === 'GET' && $action === 'summary') {
        try {
            $sql = "SELECT 
                    (SELECT COUNT(*) FROM pets WHERE is_active = TRUE) as total_pets,
                    (SELECT COUNT(*) FROM feeding_history WHERE feeding_date = CURDATE()) as today_feedings,
                    (SELECT SUM(amount_grams) FROM feeding_history WHERE feeding_date = CURDATE()) as today_food,
                    (SELECT COUNT(DISTINCT food_id) FROM feeding_history) as total_foods_used";
            
            $stmt = $db->query($sql);
            $stats = $stmt->fetch();
            
            Response::sendSuccess([
                'totalPets' => (int)$stats['total_pets'],
                'todayFeedings' => (int)$stats['today_feedings'],
                'todayFood' => (float)$stats['today_food'],
                'totalFoodsUsed' => (int)$stats['total_foods_used']
            ]);
        } catch (Exception $e) {
            Response::sendError('Ошибка при получении статистики: ' . $e->getMessage(), 500);
        }
    } else {
        Response::sendError('Метод не найден', 404);
    }
}

// ===== 404 - ENDPOINT НЕ НАЙДЕН =====
else {
    Response::sendError('Endpoint не найден: ' . $endpoint, 404);
}

error_log("=== CATFEEDER API RESPONSE SENT ===");
?>