<?php
require_once 'models/Food.php';
require_once 'utils/Response.php';

class FoodController {
    private $food;
    
    public function __construct() {
        $database = new Database();
        $db = $database->getConnection();
        $this->food = new Food($db);
    }
    
    public function getByBarcode($barcode) {
        try {
            $food = $this->food->getByBarcode($barcode);
            
            if ($food) {
                $result = [
                    'id' => $food['id'],
                    'barcode' => $food['barcode'],
                    'name' => $food['name'],
                    'manufacturer' => $food['manufacturer_name'],
                    'type' => $food['type_name'],
                    'flavor' => $food['flavor_name'],
                    'weight' => $food['weight_grams'],
                    'protein' => $food['protein_percent'],
                    'fat' => $food['fat_percent'],
                    'calories' => $food['calories'],
                    'photo' => $food['photo_path']
                ];
                
                Response::sendSuccess($result);
            } else {
                Response::sendSuccess(null, 'Корм не найден в базе');
            }
        } catch (Exception $e) {
            Response::sendError('Ошибка при поиске корма: ' . $e->getMessage(), 500);
        }
    }
    
    public function create($data) {
        try {
            if (!isset($data['barcode']) || !isset($data['name'])) {
                Response::sendError('Не указаны обязательные параметры: barcode, name');
            }
            
            $success = $this->food->create($data);
            
            if ($success) {
                $id = $this->food->conn->lastInsertId();
                Response::sendSuccess(['id' => $id], 'Корм успешно добавлен');
            } else {
                Response::sendError('Ошибка при добавлении корма');
            }
        } catch (Exception $e) {
            Response::sendError('Ошибка при добавлении корма: ' . $e->getMessage(), 500);
        }
    }
}
?>
