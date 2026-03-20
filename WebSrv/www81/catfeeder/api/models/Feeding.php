<?php
class Feeding {
    private $conn;
    private $table = 'feeding_history';
    
    public function __construct($db) {
        $this->conn = $db;
    }
    
    // Создать запись о кормлении
    public function create($data) {
        // Проверяем обязательные поля
        if (!isset($data['pet_id']) || !isset($data['amount'])) {
            return false;
        }
        
        if (!isset($data['food_id']) && !isset($data['barcode'])) {
            return false;
        }
        
        // Определяем food_id
        $foodId = null;
        
        if (isset($data['food_id'])) {
            $foodId = $data['food_id'];
        } elseif (isset($data['barcode'])) {
            // Ищем корм по штрихкоду
            $foodQuery = "SELECT id FROM foods WHERE barcode = :barcode AND is_active = TRUE LIMIT 1";
            $foodStmt = $this->conn->prepare($foodQuery);
            $foodStmt->bindParam(':barcode', $data['barcode']);
            $foodStmt->execute();
            $food = $foodStmt->fetch();
            
            if ($food) {
                $foodId = $food['id'];
            } else {
                // Создаем новый корм
                $foodName = isset($data['food_name']) && !empty($data['food_name']) 
                            ? $data['food_name'] 
                            : 'Корм ' . $data['barcode'];
                
                $insertQuery = "INSERT INTO foods 
                               (barcode, name, manufacturer_id, food_type_id, is_active, created_at)
                               VALUES 
                               (:barcode, :name, 1, 1, TRUE, NOW())";
                $insertStmt = $this->conn->prepare($insertQuery);
                $insertStmt->bindParam(':barcode', $data['barcode']);
                $insertStmt->bindParam(':name', $foodName);
                
                if ($insertStmt->execute()) {
                    $foodId = $this->conn->lastInsertId();
                }
            }
        }
        
        if (!$foodId) {
            return false;
        }
        
        // Рассчитываем калории если не указаны
        $calories = $data['calories'] ?? null;
        
        if (!$calories && isset($data['amount'])) {
            // Пробуем получить калорийность корма
            $calQuery = "SELECT calories FROM foods WHERE id = :id AND calories IS NOT NULL LIMIT 1";
            $calStmt = $this->conn->prepare($calQuery);
            $calStmt->bindParam(':id', $foodId, PDO::PARAM_INT);
            $calStmt->execute();
            $foodCal = $calStmt->fetch();
            
            if ($foodCal && $foodCal['calories'] > 0) {
                $calories = ($data['amount'] / 100) * $foodCal['calories'];
            }
        }
        
        // Определяем was_finished (по умолчанию 100% - всё съедено)
        $wasFinished = $data['was_finished'] ?? 100;
        
        // Создаем запись о кормлении
        $query = "INSERT INTO " . $this->table . " 
                  (pet_id, food_id, feeding_date, feeding_time, amount_grams, calories, was_finished, notes, created_at)
                  VALUES 
                  (:pet_id, :food_id, :feeding_date, :feeding_time, :amount_grams, :calories, :was_finished, :notes, NOW())";
        
        $stmt = $this->conn->prepare($query);
        
        $feedingDate = $data['feeding_date'] ?? date('Y-m-d');
        $feedingTime = $data['feeding_time'] ?? date('H:i:s');
        $notes = $data['notes'] ?? null;
        
        $stmt->bindParam(':pet_id', $data['pet_id'], PDO::PARAM_INT);
        $stmt->bindParam(':food_id', $foodId, PDO::PARAM_INT);
        $stmt->bindParam(':feeding_date', $feedingDate);
        $stmt->bindParam(':feeding_time', $feedingTime);
        $stmt->bindParam(':amount_grams', $data['amount']);
        $stmt->bindParam(':calories', $calories);
        $stmt->bindParam(':was_finished', $wasFinished, PDO::PARAM_INT);
        $stmt->bindParam(':notes', $notes);
        
        if ($stmt->execute()) {
            return $this->conn->lastInsertId();
        }
        
        return false;
    }
     // Обновить статус съеденного
    public function updateWasFinished($id, $wasFinished) {
        $query = "UPDATE " . $this->table . " 
                  SET was_finished = :was_finished, updated_at = NOW()
                  WHERE id = :id";
        
        $stmt = $this->conn->prepare($query);
        $stmt->bindParam(':id', $id, PDO::PARAM_INT);
        $stmt->bindParam(':was_finished', $wasFinished, PDO::PARAM_INT);
        
        return $stmt->execute();
    }
    
    // Получить историю кормлений для питомца
    public function getHistory($petId, $startDate = null, $endDate = null, $limit = 100, $offset = 0) {
        $query = "SELECT fh.*, f.name as food_name, f.barcode, m.name as manufacturer
                  FROM " . $this->table . " fh
                  JOIN foods f ON fh.food_id = f.id
                  JOIN manufacturers m ON f.manufacturer_id = m.id
                  WHERE fh.pet_id = :pet_id";
        
        $params = [':pet_id' => $petId];
        
        if ($startDate) {
            $query .= " AND fh.feeding_date >= :start_date";
            $params[':start_date'] = $startDate;
        }
        
        if ($endDate) {
            $query .= " AND fh.feeding_date <= :end_date";
            $params[':end_date'] = $endDate;
        }
        
        $query .= " ORDER BY fh.feeding_date DESC, fh.feeding_time DESC 
                    LIMIT :limit OFFSET :offset";
        
        $params[':limit'] = (int)$limit;
        $params[':offset'] = (int)$offset;
        
        $stmt = $this->conn->prepare($query);
        
        foreach ($params as $key => $value) {
            if (is_int($value)) {
                $stmt->bindValue($key, $value, PDO::PARAM_INT);
            } else {
                $stmt->bindValue($key, $value);
            }
        }
        
        $stmt->execute();
        return $stmt->fetchAll();
    }
    // Получить историю кормлений по дате
    public function getByDate($date) {
        $query = "SELECT fh.*, f.name as food_name, p.name as pet_name
                  FROM " . $this->table . " fh
                  JOIN foods f ON fh.food_id = f.id
                  JOIN pets p ON fh.pet_id = p.id
                  WHERE fh.feeding_date = :date
                  ORDER BY fh.feeding_time DESC";
        
        $stmt = $this->conn->prepare($query);
        $stmt->bindParam(':date', $date);
        $stmt->execute();
        
        return $stmt->fetchAll();
    }
    
    // Получить конкретное кормление по ID
    public function getById($id) {
        $query = "SELECT fh.*, f.name as food_name, f.barcode, m.name as manufacturer, p.name as pet_name
                  FROM " . $this->table . " fh
                  JOIN foods f ON fh.food_id = f.id
                  JOIN manufacturers m ON f.manufacturer_id = m.id
                  JOIN pets p ON fh.pet_id = p.id
                  WHERE fh.id = :id
                  LIMIT 1";
        
        $stmt = $this->conn->prepare($query);
        $stmt->bindParam(':id', $id, PDO::PARAM_INT);
        $stmt->execute();
        
        return $stmt->fetch();
    }
    
    // Получить статистику за период
    public function getStats($petId = null, $startDate = null, $endDate = null) {
        $query = "SELECT 
                    COUNT(*) as total_feedings,
                    SUM(amount_grams) as total_food,
                    AVG(amount_grams) as avg_amount,
                    MIN(amount_grams) as min_amount,
                    MAX(amount_grams) as max_amount,
                    COUNT(DISTINCT feeding_date) as total_days,
                    COUNT(DISTINCT food_id) as total_foods
                  FROM " . $this->table . "
                  WHERE 1=1";
        
        $params = [];
        
        if ($petId) {
            $query .= " AND pet_id = :pet_id";
            $params[':pet_id'] = $petId;
        }
        
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
    
    // Получить любимые корма питомца
    public function getFavoriteFoods($petId, $limit = 5) {
        $query = "SELECT 
                    f.id, f.name, f.barcode, m.name as manufacturer,
                    COUNT(*) as feedings_count,
                    SUM(amount_grams) as total_amount
                  FROM " . $this->table . " fh
                  JOIN foods f ON fh.food_id = f.id
                  JOIN manufacturers m ON f.manufacturer_id = m.id
                  WHERE fh.pet_id = :pet_id
                  GROUP BY f.id, f.name, f.barcode, m.name
                  ORDER BY feedings_count DESC
                  LIMIT :limit";
        
        $stmt = $this->conn->prepare($query);
        $stmt->bindParam(':pet_id', $petId, PDO::PARAM_INT);
        $stmt->bindParam(':limit', $limit, PDO::PARAM_INT);
        $stmt->execute();
        
        return $stmt->fetchAll();
    }
    
    // Получить статистику по часам
    public function getHourlyStats($petId = null) {
        $query = "SELECT 
                    HOUR(feeding_time) as hour,
                    COUNT(*) as feedings_count,
                    AVG(amount_grams) as avg_amount
                  FROM " . $this->table . "
                  WHERE 1=1";
        
        $params = [];
        
        if ($petId) {
            $query .= " AND pet_id = :pet_id";
            $params[':pet_id'] = $petId;
        }
        
        $query .= " GROUP BY HOUR(feeding_time)
                    ORDER BY hour";
        
        $stmt = $this->conn->prepare($query);
        $stmt->execute($params);
        
        return $stmt->fetchAll();
    }
    
    // Обновить запись о кормлении
    public function update($id, $data) {
        $query = "UPDATE " . $this->table . " 
                  SET amount_grams = :amount_grams,
                      calories = :calories,
                      notes = :notes,
                      updated_at = NOW()
                  WHERE id = :id";
        
        $stmt = $this->conn->prepare($query);
        
        $calories = $data['calories'] ?? null;
        $notes = $data['notes'] ?? null;
        
        $stmt->bindParam(':id', $id, PDO::PARAM_INT);
        $stmt->bindParam(':amount_grams', $data['amount']);
        $stmt->bindParam(':calories', $calories);
        $stmt->bindParam(':notes', $notes);
        
        return $stmt->execute();
    }
    
    // Удалить запись о кормлении
    public function delete($id) {
        $query = "DELETE FROM " . $this->table . " WHERE id = :id";
        
        $stmt = $this->conn->prepare($query);
        $stmt->bindParam(':id', $id, PDO::PARAM_INT);
        
        return $stmt->execute();
    }
}
?>
