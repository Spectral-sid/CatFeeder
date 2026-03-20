<?php
class Pet {
    private $conn;
    private $table = 'pets';
    
    public $id;
    public $name;
    public $breed_id;
    public $gender;
    public $birth_date;
    public $color;
    public $current_weight;
    public $target_weight;
    public $is_active;
    
    public function __construct($db) {
        $this->conn = $db;
    }
    
    // Получить всех питомцев
    public function getAll() {
        $query = "SELECT p.*, cb.name as breed_name 
                  FROM " . $this->table . " p 
                  LEFT JOIN cat_breeds cb ON p.breed_id = cb.id 
                  WHERE p.is_active = TRUE 
                  ORDER BY p.name";
        
        $stmt = $this->conn->prepare($query);
        $stmt->execute();
        
        return $stmt;
    }
    
    // Получить питомца по ID
    public function getById($id) {
        $query = "SELECT p.*, cb.name as breed_name 
                  FROM " . $this->table . " p 
                  LEFT JOIN cat_breeds cb ON p.breed_id = cb.id 
                  WHERE p.id = :id AND p.is_active = TRUE";
        
        $stmt = $this->conn->prepare($query);
        $stmt->bindParam(':id', $id, PDO::PARAM_INT);
        $stmt->execute();
        
        return $stmt->fetch();
    }
    
    // Создать нового питомца
    public function create($data) {
        $query = "INSERT INTO " . $this->table . " 
                  (name, breed_id, gender, birth_date, color, 
                   current_weight, target_weight, notes, profile_photo_path, created_at)
                  VALUES 
                  (:name, :breed_id, :gender, :birth_date, :color,
                   :current_weight, :target_weight, :notes, :profile_photo_path, NOW())";
        
        $stmt = $this->conn->prepare($query);
        
        // Очистка и привязка данных
        $stmt->bindParam(':name', $data['name']);
        $stmt->bindParam(':breed_id', $data['breed_id'], PDO::PARAM_INT);
        $stmt->bindParam(':gender', $data['gender']);
        $stmt->bindParam(':birth_date', $data['birth_date']);
        $stmt->bindParam(':color', $data['color']);
        $stmt->bindParam(':current_weight', $data['current_weight']);
        $stmt->bindParam(':target_weight', $data['target_weight']);
        $stmt->bindParam(':notes', $data['notes']);
        $stmt->bindParam(':profile_photo_path', $data['profile_photo_path']);
        
        if ($stmt->execute()) {
            return $this->conn->lastInsertId();
        }
        
        return false;
    }
    
    // Обновить данные питомца
    public function update($id, $data) {
        $query = "UPDATE " . $this->table . " 
                  SET name = :name,
                      breed_id = :breed_id,
                      gender = :gender,
                      birth_date = :birth_date,
                      color = :color,
                      current_weight = :current_weight,
                      target_weight = :target_weight,
                      notes = :notes,
                      profile_photo_path = :profile_photo_path,
                      updated_at = NOW()
                  WHERE id = :id";
        
        $stmt = $this->conn->prepare($query);
        
        $stmt->bindParam(':id', $id, PDO::PARAM_INT);
        $stmt->bindParam(':name', $data['name']);
        $stmt->bindParam(':breed_id', $data['breed_id'], PDO::PARAM_INT);
        $stmt->bindParam(':gender', $data['gender']);
        $stmt->bindParam(':birth_date', $data['birth_date']);
        $stmt->bindParam(':color', $data['color']);
        $stmt->bindParam(':current_weight', $data['current_weight']);
        $stmt->bindParam(':target_weight', $data['target_weight']);
        $stmt->bindParam(':notes', $data['notes']);
        $stmt->bindParam(':profile_photo_path', $data['profile_photo_path']);
        
        return $stmt->execute();
    }
    
    // Добавить запись о весе
    public function addWeight($petId, $weight, $date, $notes = null) {
        // Сначала добавляем запись в историю веса
        $query = "INSERT INTO weight_history 
                  (pet_id, weight, measurement_date, notes, created_at)
                  VALUES 
                  (:pet_id, :weight, :measurement_date, :notes, NOW())
                  ON DUPLICATE KEY UPDATE
                  weight = VALUES(weight),
                  notes = VALUES(notes),
                  created_at = NOW()";
        
        $stmt = $this->conn->prepare($query);
        $stmt->bindParam(':pet_id', $petId, PDO::PARAM_INT);
        $stmt->bindParam(':weight', $weight);
        $stmt->bindParam(':measurement_date', $date);
        $stmt->bindParam(':notes', $notes);
        
        if ($stmt->execute()) {
            // Обновляем текущий вес питомца
            $updateQuery = "UPDATE " . $this->table . " 
                           SET current_weight = :weight, 
                               updated_at = NOW() 
                           WHERE id = :id";
            $updateStmt = $this->conn->prepare($updateQuery);
            $updateStmt->bindParam(':weight', $weight);
            $updateStmt->bindParam(':id', $petId, PDO::PARAM_INT);
            $updateStmt->execute();
            
            return $this->conn->lastInsertId();
        }
        
        return false;
    }
    
    // Получить историю веса
    public function getWeightHistory($petId) {
        $query = "SELECT * FROM weight_history 
                  WHERE pet_id = :pet_id 
                  ORDER BY measurement_date DESC 
                  LIMIT 30";
        
        $stmt = $this->conn->prepare($query);
        $stmt->bindParam(':pet_id', $petId, PDO::PARAM_INT);
        $stmt->execute();
        
        return $stmt;
    }
    
    // Получить статистику кормлений
    public function getFeedingStats($petId, $startDate = null, $endDate = null) {
        $query = "SELECT 
                    COUNT(*) as feeding_count,
                    SUM(amount_grams) as total_food,
                    AVG(amount_grams) as avg_amount,
                    MIN(feeding_date) as first_date,
                    MAX(feeding_date) as last_date
                  FROM feeding_history 
                  WHERE pet_id = :pet_id";
        
        $params = [':pet_id' => $petId];
        
        if ($startDate) {
            $query .= " AND feeding_date >= :start_date";
            $params[':start_date'] = $startDate;
        }
        
        if ($endDate) {
            $query .= " AND feeding_date <= :end_date";
            $params[':end_date'] = $endDate;
        }
        
        $stmt = $this->conn->prepare($query);
        $stmt->execute($params);
        
        return $stmt->fetch();
    }
    
    // Удалить питомца (мягкое удаление)
    public function delete($id) {
        $query = "UPDATE " . $this->table . " 
                  SET is_active = FALSE, 
                      updated_at = NOW() 
                  WHERE id = :id";
        
        $stmt = $this->conn->prepare($query);
        $stmt->bindParam(':id', $id, PDO::PARAM_INT);
        
        return $stmt->execute();
    }
}
?>
