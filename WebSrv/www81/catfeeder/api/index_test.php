<?php
// Включаем вывод ошибок для отладки
error_reporting(E_ALL);
ini_set('display_errors', 1);

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

// Получаем путь запроса
$request_uri = $_SERVER['REQUEST_URI'];
$script_name = $_SERVER['SCRIPT_NAME'];

// Отладочная информация
error_log("Request URI: " . $request_uri);
error_log("Script Name: " . $script_name);

// Убираем базовый путь из URI
$base_path = dirname($script_name);
$path = str_replace($base_path, '', $request_uri);

// Убираем параметры запроса
$path = parse_url($path, PHP_URL_PATH);
$path = trim($path, '/');

// Отладочная информация
error_log("Parsed path: " . $path);

// Разбиваем путь на части
$path_parts = $path ? explode('/', $path) : [];

// Определяем endpoint и action
$endpoint = $path_parts[0] ?? '';
$action = $path_parts[1] ?? '';
$param = $path_parts[2] ?? '';

// Отладочная информация
error_log("Endpoint: " . $endpoint);
error_log("Action: " . $action);
error_log("Param: " . $param);

// Простой роутер
switch ($endpoint) {
    case '':
        // Корневой путь - health check
        echo json_encode([
            'success' => true,
            'message' => 'API работает нормально',
            'timestamp' => date('Y-m-d H:i:s'),
            'request_info' => [
                'path' => $path,
                'endpoint' => $endpoint,
                'action' => $action,
                'param' => $param,
                'method' => $_SERVER['REQUEST_METHOD']
            ]
        ]);
        break;
        
    case 'health':
        echo json_encode([
            'success' => true,
            'message' => 'API здоров!',
            'timestamp' => date('Y-m-d H:i:s'),
            'status' => 'online',
            'server' => $_SERVER['SERVER_NAME'],
            'port' => $_SERVER['SERVER_PORT']
        ]);
        break;
        
    case 'pets':
        // Проверяем метод запроса
        if ($_SERVER['REQUEST_METHOD'] === 'GET') {
            // Тестовые данные питомцев
            echo json_encode([
                'success' => true,
                'data' => [
                    [
                        'id' => 1,
                        'name' => 'Мурзик',
                        'breed' => 'Дворовый',
                        'gender' => 'Самец',
                        'birthDate' => '2020-05-15',
                        'currentWeight' => 4.5,
                        'targetWeight' => 4.3,
                        'profilePhoto' => null
                    ],
                    [
                        'id' => 2,
                        'name' => 'Васька',
                        'breed' => 'Сиамский',
                        'gender' => 'Самец',
                        'birthDate' => '2019-11-20',
                        'currentWeight' => 5.2,
                        'targetWeight' => 5.0,
                        'profilePhoto' => null
                    ],
                    [
                        'id' => 3,
                        'name' => 'Мурка',
                        'breed' => 'Британская',
                        'gender' => 'Самка',
                        'birthDate' => '2021-03-10',
                        'currentWeight' => 3.8,
                        'targetWeight' => 3.5,
                        'profilePhoto' => null
                    ]
                ],
                'total' => 3,
                'timestamp' => date('Y-m-d H:i:s')
            ]);
        } elseif ($_SERVER['REQUEST_METHOD'] === 'POST') {
            // Обработка POST запроса
            $input = json_decode(file_get_contents('php://input'), true);
            echo json_encode([
                'success' => true,
                'message' => 'Питомец создан',
                'data' => [
                    'id' => 4,
                    'name' => $input['name'] ?? 'Новый питомец',
                    'created_at' => date('Y-m-d H:i:s')
                ]
            ]);
        } else {
            http_response_code(405);
            echo json_encode([
                'success' => false,
                'error' => 'Метод не разрешен',
                'allowed_methods' => ['GET', 'POST']
            ]);
        }
        break;
        
    case 'foods':
        if ($action === 'barcode' && $param) {
            // Поиск корма по штрихкоду
            echo json_encode([
                'success' => true,
                'data' => [
                    'id' => 123,
                    'barcode' => $param,
                    'name' => 'Purina One для кошек',
                    'manufacturer' => 'Purina',
                    'type' => 'Сухой корм',
                    'flavor' => 'Курица',
                    'weight' => 1000.00,
                    'protein' => 34.0,
                    'fat' => 15.0,
                    'calories' => 380.50,
                    'photo' => null
                ],
                'message' => 'Корм найден'
            ]);
        } elseif ($_SERVER['REQUEST_METHOD'] === 'POST') {
            // Создание нового корма
            $input = json_decode(file_get_contents('php://input'), true);
            echo json_encode([
                'success' => true,
                'message' => 'Корм добавлен',
                'data' => [
                    'id' => 456,
                    'barcode' => $input['barcode'] ?? '0000000000000',
                    'name' => $input['name'] ?? 'Новый корм'
                ]
            ]);
        } else {
            http_response_code(404);
            echo json_encode([
                'success' => false,
                'error' => 'Endpoint не найден: ' . $endpoint . '/' . $action,
                'available_endpoints' => ['/foods/barcode/{штрихкод}']
            ]);
        }
        break;
        
    case 'feeding':
        if ($_SERVER['REQUEST_METHOD'] === 'GET' && $action === 'history' && $param) {
            // История кормлений для питомца
            echo json_encode([
                'success' => true,
                'data' => [
                    [
                        'id' => 1,
                        'date' => date('Y-m-d'),
                        'time' => '08:30:00',
                        'foodName' => 'Purina One для кошек',
                        'barcode' => '5901234123456',
                        'amount' => 50.0,
                        'calories' => 190.25,
                        'notes' => 'Утреннее кормление'
                    ],
                    [
                        'id' => 2,
                        'date' => date('Y-m-d', strtotime('-1 day')),
                        'time' => '18:00:00',
                        'foodName' => 'Whiskas влажный корм',
                        'barcode' => '5901234123457',
                        'amount' => 85.0,
                        'calories' => 120.50,
                        'notes' => 'Вечернее кормление'
                    ]
                ],
                'petId' => $param,
                'total' => 2
            ]);
        } elseif ($_SERVER['REQUEST_METHOD'] === 'POST') {
            // Запись кормления
            $input = json_decode(file_get_contents('php://input'), true);
            echo json_encode([
                'success' => true,
                'message' => 'Кормление записано',
                'data' => [
                    'id' => 789,
                    'petId' => $input['petId'] ?? 1,
                    'amount' => $input['amount'] ?? 0,
                    'timestamp' => date('Y-m-d H:i:s')
                ]
            ]);
        } else {
            http_response_code(404);
            echo json_encode([
                'success' => false,
                'error' => 'Endpoint не найден',
                'available_endpoints' => ['POST /feeding', 'GET /feeding/history/{petId}']
            ]);
        }
        break;
        
    default:
        http_response_code(404);
        echo json_encode([
            'success' => false,
            'error' => 'Endpoint не найден: ' . $endpoint,
            'timestamp' => date('Y-m-d H:i:s'),
            'request_info' => [
                'path' => $path,
                'endpoint' => $endpoint,
                'action' => $action,
                'param' => $param
            ],
            'available_endpoints' => [
                '/',
                '/health',
                '/pets',
                '/foods/barcode/{штрихкод}',
                '/feeding',
                '/feeding/history/{petId}'
            ]
        ]);
        break;
}
?>
