<?php
require_once 'models/Feeding.php';
require_once 'utils/Response.php';

class FeedingController {
    private $feeding;
    
    public function __construct() {
        $database = new Database();
        $db = $database->getConnection();
        $this->feeding = new Feeding($db);
    }
    
    public function create($data) {
        try {
            if (!isset($data['pet_id']) || !isset($data['amount'])) {
                Response::sendError('Не указаны обязательные параметры: pet_id, amount');
            }
            
            if (!isset($data['food_id']) && !isset($data['barcode'])) {
                Response::sendError('Не указан корм (food_id или barcode)');
            }
            
            $success = $this->feeding->create($data);
            
            if ($success) {
                $id = $this->feeding->conn->lastInsertId();
                Response::sendSuccess(['id' => $id], 'Кормление успешно записано');
            } else {
                Response::sendError('Ошибка при записи кормления');
            }
        } catch (Exception $e) {
            Response::sendError('Ошибка при записи кормления: ' . $e->getMessage(), 500);
        }
    }
    
    public function getHistory($petId) {
        try {
            $limit = $_GET['limit'] ?? 50;
            $offset = $_GET['offset'] ?? 0;
            
            $stmt = $this->feeding->getHistory($petId, $limit, $offset);
            $history = $stmt->fetchAll();
            
            $result = array_map(function($item) {
                return [
                    'id' => $item['id'],
                    'date' => $item['feeding_date'],
                    'time' => $item['feeding_time'],
                    'foodName' => $item['food_name'],
                    'barcode' => $item['barcode'],
                    'manufacturer' => $item['manufacturer'],
                    'amount' => $item['amount_grams'],
                    'calories' => $item['calories'],
                    'notes' => $item['notes']
                ];
            }, $history);
            
            Response::sendSuccess($result);
        } catch (Exception $e) {
            Response::sendError('Ошибка при получении истории: ' . $e->getMessage(), 500);
        }
    }
}
?>
