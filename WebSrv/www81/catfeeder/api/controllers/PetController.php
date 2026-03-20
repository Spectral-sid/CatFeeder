<?php

// Отладочная информация

$request_uri = $_SERVER['REQUEST_URI'];
$script_name = $_SERVER['SCRIPT_NAME'];
error_log("PetcontrRequest URI: " . $request_uri);
error_log("Script Name: " . $script_name);

//require_once '../models/Pet.php';
require_once 'models/Pet.php';
require_once 'utils/Response.php';

//require_once '../utils/Response.php';

class PetController {
    private $pet;
    
    public function __construct() {
        $database = new Database();
        $db = $database->getConnection();
        $this->pet = new Pet($db);
    }
    
    public function getAll() {
        try {
            $stmt = $this->pet->getAll();
            $pets = $stmt->fetchAll();
            
            $result = array_map(function($pet) {
                return [
                    'id' => $pet['id'],
                    'name' => $pet['name'],
                    'breed' => $pet['breed_name'] ?? 'Не указана',
                    'gender' => $pet['gender'] === 'male' ? 'Самец' : 'Самка',
                    'birthDate' => $pet['birth_date'],
                    'currentWeight' => $pet['current_weight'],
                    'targetWeight' => $pet['target_weight'],
                    'profilePhoto' => $pet['profile_photo_path'],
                    'isActive' => (bool)$pet['is_active']
                ];
            }, $pets);
            
            Response::sendSuccess($result);
        } catch (Exception $e) {
            Response::sendError('Ошибка при получении питомцев: ' . $e->getMessage(), 500);
        }
    }
    
    public function getById($id) {
        try {
            $pet = $this->pet->getById($id);
            
            if ($pet) {
                $result = [
                    'id' => $pet['id'],
                    'name' => $pet['name'],
                    'breed' => $pet['breed_name'] ?? 'Не указана',
                    'gender' => $pet['gender'] === 'male' ? 'Самец' : 'Самка',
                    'birthDate' => $pet['birth_date'],
                    'currentWeight' => $pet['current_weight'],
                    'targetWeight' => $pet['target_weight'],
                    'profilePhoto' => $pet['profile_photo_path']
                ];
                
                Response::sendSuccess($result);
            } else {
                Response::sendError('Питомец не найден', 404);
            }
        } catch (Exception $e) {
            Response::sendError('Ошибка при получении питомца: ' . $e->getMessage(), 500);
        }
    }
    
    public function addWeight($data) {
        try {
            if (!isset($data['petId']) || !isset($data['weight'])) {
                Response::sendError('Не указаны обязательные параметры: petId, weight');
            }
            
            $petId = $data['petId'];
            $weight = $data['weight'];
            $date = $data['date'] ?? date('Y-m-d');
            $notes = $data['notes'] ?? null;
            
            $id = $this->pet->addWeight($petId, $weight, $date, $notes);
            
            if ($id) {
                Response::sendSuccess(['id' => $id], 'Вес успешно записан');
            } else {
                Response::sendError('Ошибка при записи веса');
            }
        } catch (Exception $e) {
            Response::sendError('Ошибка при записи веса: ' . $e->getMessage(), 500);
        }
    }
    
    public function getHistory($petId) {
        try {
            $stmt = $this->pet->getWeightHistory($petId);
            $history = $stmt->fetchAll();
            
            Response::sendSuccess($history);
        } catch (Exception $e) {
            Response::sendError('Ошибка при получении истории веса: ' . $e->getMessage(), 500);
        }
    }
}
?>
